package org.verapdf.metadata.fixer.impl.pb.model;

import static org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus.FIX_ERROR;
import static org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus.NO_ACTION;
import static org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus.SUCCESS;
import static org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus.WONT_FIX;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.verapdf.metadata.fixer.entity.InfoDictionary;
import org.verapdf.metadata.fixer.entity.Metadata;
import org.verapdf.metadata.fixer.entity.PDFDocument;
import org.verapdf.pdfa.results.MetadataFixerResult;
import org.verapdf.pdfa.results.MetadataFixerResultImpl;

/**
 * @author Evgeniy Muravitskiy
 */
public class PDFDocumentImpl implements PDFDocument {

	private static final Logger LOGGER = Logger.getLogger(PDFDocumentImpl.class);

	private final PDDocument document;
	private MetadataImpl metadata;
	private InfoDictionaryImpl info;

	/**
	 * @param document
	 */
	public PDFDocumentImpl(PDDocument document) {
		if (document == null) {
			throw new IllegalArgumentException("Document representation can not be null");
		}
		this.document = document;
		this.metadata = parseMetadata();
		this.info = this.getInfo();
	}

	private MetadataImpl parseMetadata() {
		PDDocumentCatalog catalog = this.document.getDocumentCatalog();
		PDMetadata meta = catalog.getMetadata();
		if (meta == null) {
			COSStream stream = this.document.getDocument().createCOSStream();
			catalog.setMetadata(new PDMetadata(stream));
			catalog.getCOSObject().setNeedToBeUpdated(true);
			XMPMetadata xmp = XMPMetadata.createXMPMetadata();
			return new MetadataImpl(xmp, stream);
		}
        return parseMetadata(meta);
	}

	private MetadataImpl parseMetadata(PDMetadata meta) {
		try {
			DomXmpParser xmpParser = new DomXmpParser();
			XMPMetadata xmp = xmpParser.parse(meta.getStream().getUnfilteredStream());
			if (xmp != null) {
				return new MetadataImpl(xmp, meta.getStream());
			}
		} catch (IOException e) {
			LOGGER.error(
					"Problems with document parsing or structure. "
							+ e.getMessage(), e);
		} catch (XmpParsingException e) {
			LOGGER.error("Problems with XMP parsing. " + e.getMessage(), e);
		}
		return null;
	}

	private InfoDictionaryImpl getInfo() {
		COSDictionary trailer = this.document.getDocument().getTrailer();
		COSBase infoDict = trailer.getDictionaryObject(COSName.INFO);
		return !(infoDict instanceof COSDictionary) ? null :
				new InfoDictionaryImpl(new PDDocumentInformation((COSDictionary) infoDict));
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public Metadata getMetadata() {
		return this.metadata;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public InfoDictionary getInfoDictionary() {
		return this.info;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public boolean isNeedToBeUpdated() {
		boolean metaUpd = this.metadata != null && this.metadata.isNeedToBeUpdated();
		boolean infoUpd = this.info != null && this.info.isNeedToBeUpdated();
		return metaUpd || infoUpd;
	}

	/**
	 * {@inheritDoc} Implemented by Apache PDFBox library.
	 */
	@Override
	public MetadataFixerResult saveDocumentIncremental(final MetadataFixerResultImpl.RepairStatus status, OutputStream output) {
	    MetadataFixerResultImpl.Builder builder = new MetadataFixerResultImpl.Builder();
		try {
			PDMetadata meta = this.document.getDocumentCatalog().getMetadata();
			boolean isMetaPresent = meta != null && this.isNeedToBeUpdated();
			boolean isMetaAdd = meta == null && this.metadata != null;
			if (isMetaPresent || isMetaAdd) {
				this.metadata.updateMetadataStream();
				if (isMetaAdd) {
					this.document.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
				}
				this.document.saveIncremental(output);
				output.close();
				builder.status(getStatus(status));
			} else {
			    builder.status(NO_ACTION);
			}
		} catch (Exception e) {
			LOGGER.info(e);
			builder.status(FIX_ERROR).addFix("Problems with document save. " + e.getMessage());
		}
		return builder.build();
	}

	private static MetadataFixerResultImpl.RepairStatus getStatus(final MetadataFixerResultImpl.RepairStatus status) {
		return status != WONT_FIX ? SUCCESS : WONT_FIX;
	}

}
