package org.verapdf.model.tools.xmp;

import com.adobe.xmp.impl.VeraPDFXMPNode;
import com.adobe.xmp.impl.XMPSchemaRegistryImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Maksim Bezrukov
 */
public class ValidatorsContainerCreator {

    public static final ValidatorsContainer PREDEFINED_CONTAINER_FOR_PDFA_1 = createValidatorsContainerPredefinedForPDFA_1();
    public static final ValidatorsContainer PREDEFINED_CONTAINER_FOR_PDFA_2_3 = createValidatorsContainerPredefinedForPDFA_2_3();

    private static ValidatorsContainer createValidatorsContainerPredefinedForPDFA_1() {
        return createBasicValidatorsContainer();
    }

    private static ValidatorsContainer createValidatorsContainerPredefinedForPDFA_2_3() {
        ValidatorsContainer container = createBasicValidatorsContainer();
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.COLORANT,
                XMPConstants.COLORANT_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.COLORANT_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeForContainer(XMPConstants.FONT, XMPConstants.FONT_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.BEAT_SPLICE_STRETCH, XMPConstants.BEAT_SPLICE_STRETCH_STRUCTURE, container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.MARKER,
                XMPConstants.MARKER_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.MARKER_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeForContainer(XMPConstants.MEDIA, XMPConstants.MEDIA_STRUCTURE, container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.PROJECT_LINK,
                XMPConstants.PROJECT_LINK_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.PROJECT_LINK_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.RESAMPLE_STRETCH,
                XMPConstants.RESAMPLE_STRETCH_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.RESAMPLE_STRETCH_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeForContainer(XMPConstants.TIME, XMPConstants.TIME_STRUCTURE, container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.TIMECODE,
                XMPConstants.TIMECODE_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.TIMECODE_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.TIME_SCALE_STRETCH,
                XMPConstants.TIME_SCALE_STRETCH_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.TIME_SCALE_STRETCH_CLOSED_CHOICE_STRUCTURE,
                container);
        return container;
    }

    private static ValidatorsContainer createBasicValidatorsContainer() {
        ValidatorsContainer container = new ValidatorsContainer();
        registerStructureTypeForContainer(XMPConstants.DIMENSIONS, XMPConstants.DIMENSIONS_STRUCTURE, container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.THUMBNAIL,
                XMPConstants.THUMBNAIL_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.THUMBNAIL_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeForContainer(XMPConstants.RESOURCE_EVENT, XMPConstants.RESOURCE_EVENT_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.RESOURCE_REF, XMPConstants.RESOURCE_REF_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.VERSION, XMPConstants.VERSION_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.JOB, XMPConstants.JOB_STRUCTURE, container);
        registerStructureTypeWithClosedChoiceForContainer(
                XMPConstants.FLASH,
                XMPConstants.FLASH_WITHOUT_CLOSED_CHOICE_STRUCTURE,
                XMPConstants.FLASH_CLOSED_CHOICE_STRUCTURE,
                container);
        registerStructureTypeForContainer(XMPConstants.OECF_SFR, XMPConstants.OECF_SFR_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.CFA_PATTERN, XMPConstants.CFA_PATTERN_STRUCTURE, container);
        registerStructureTypeForContainer(XMPConstants.DEVICE_SETTINGS, XMPConstants.DEVICE_SETTINGS_STRUCTURE, container);
        return container;
    }

    static ValidatorsContainer createExtendedValidatorsContainerForPDFA_1(VeraPDFXMPNode extensionContainer) {
        ValidatorsContainer container = createValidatorsContainerPredefinedForPDFA_1();
        return createExtendedValidatorsContainer(extensionContainer, container);
    }

    static ValidatorsContainer createExtendedValidatorsContainerForPDFA_2_3(VeraPDFXMPNode extensionContainer) {
        ValidatorsContainer container = createValidatorsContainerPredefinedForPDFA_2_3();
        return createExtendedValidatorsContainer(extensionContainer, container);
    }

    private static ValidatorsContainer createExtendedValidatorsContainer(VeraPDFXMPNode schemasDefinitions, ValidatorsContainer container) {
        List<VeraPDFXMPNode> schemas = schemasDefinitions.getChildren();
        for (VeraPDFXMPNode node : schemas) {
            registerAllTypesFromExtensionSchemaNode(node, container);
        }
        return container;
    }

    private static void registerAllTypesFromExtensionSchemaNode(VeraPDFXMPNode schema, ValidatorsContainer container) {
        List<VeraPDFXMPNode> schemaChildren = schema.getChildren();
        for (int i = schemaChildren.size() - 1; i >= 0; --i) {
            VeraPDFXMPNode child = schemaChildren.get(i);
            if (XMPSchemaRegistryImpl.NS_PDFA_SCHEMA.equals(child.getNamespaceURI()) && "valueType".equals(child.getName())) {
                if (child.getOptions().isArray()) {
                    registerAllTypesFromValueTypeArrayNode(child, container);
                }
                break;
            }
        }
    }

    private static void registerAllTypesFromValueTypeArrayNode(VeraPDFXMPNode valueTypes, ValidatorsContainer container) {
        List<VeraPDFXMPNode> children = valueTypes.getChildren();
        for (VeraPDFXMPNode node : children) {
            registerTypeNode(node, container);
        }
    }

    private static void registerTypeNode(VeraPDFXMPNode valueType, ValidatorsContainer container) {
        String name = null;
        String namespace = null;
        Map<String, String> fields = null;

        for (VeraPDFXMPNode child : valueType.getChildren()) {
            if (XMPSchemaRegistryImpl.NS_PDFA_TYPE.equals(child.getNamespaceURI())) {
                switch (child.getName()) {
                    case "type":
                        name = child.getValue();
                        break;
                    case "namespaceURI":
                        namespace = child.getValue();
                        break;
                    case "field":
                        if (child.getOptions().isArray()) {
                            fields = getStructureMapFromFieldsNode(child);
                        }
                        break;
                }
            }
        }

        if (name != null && namespace != null && fields != null && !fields.isEmpty()) {
            container.registerValidator(name, namespace, fields);
        }
    }

    private static Map<String, String> getStructureMapFromFieldsNode(VeraPDFXMPNode node) {
        Map<String, String> res = new HashMap<>();

        List<VeraPDFXMPNode> fields = node.getChildren();
        for (VeraPDFXMPNode field : fields) {
            String name = null;
            String valueType = null;

            for (VeraPDFXMPNode child : field.getChildren()) {
                if (XMPSchemaRegistryImpl.NS_PDFA_FIELD.equals(child.getNamespaceURI())) {
                    switch (child.getName()) {
                        case "name":
                            name = child.getValue();
                            break;
                        case "valueType":
                            valueType = child.getValue();
                            break;
                    }
                }
            }
            if (name != null && valueType != null) {
                res.put(name, valueType);
            }
        }

        return res;
    }

    private static void registerStructureTypeForContainer(String structureType, String[] structure, ValidatorsContainer container) {
        Map<String, String> res = new HashMap<>();
        for (int i = 1; i < structure.length; i += 2) {
            res.put(structure[i], structure[i + 1]);
        }
        container.registerValidator(structureType, structure[0], res);
    }

    private static void registerStructureTypeWithClosedChoiceForContainer(String structureType, String[] structure, String[] closedStructure, ValidatorsContainer container) {
        Map<String, String> res = new HashMap<>();
        for (int i = 1; i < structure.length; i += 2) {
            res.put(structure[i], structure[i + 1]);
        }
        Map<String, Pattern> closedRes = new HashMap<>();
        for (int i = 0; i < closedStructure.length; i += 2) {
            closedRes.put(closedStructure[i], Pattern.compile(closedStructure[i + 1]));
        }
        container.registerClosedChoiceValidator(structureType, structure[0], res, closedRes);
    }
}