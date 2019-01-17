package org.grobid.core.data;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.grobid.core.GrobidModels;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.grobid.core.document.xml.XmlBuilderUtils;
import org.grobid.core.document.Document;
import org.grobid.core.document.TEIFormatter;
import org.grobid.core.engines.config.GrobidAnalysisConfig;
import org.grobid.core.layout.BoundingBox;
import org.grobid.core.layout.GraphicObject;
import org.grobid.core.layout.GraphicObjectType;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.LayoutTokensUtil;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.tokenization.TaggingTokenCluster;
import org.grobid.core.tokenization.TaggingTokenClusteror;
import org.grobid.core.utilities.KeyGen;
import org.grobid.core.engines.label.TaggingLabels;
import org.grobid.core.engines.label.TaggingLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import static org.grobid.core.document.xml.XmlBuilderUtils.teiElement;
import static org.grobid.core.document.xml.XmlBuilderUtils.addXmlId;
import static org.grobid.core.document.xml.XmlBuilderUtils.textNode;

/**
 * Class for representing a figure.
 *
 * @author Patrice Lopez
 */
public class Figure {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Figure.class);

    public static final Predicate<GraphicObject> GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.BITMAP;
        }
    };

    public static final Predicate<GraphicObject> VECTOR_BOX_GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.VECTOR_BOX;
        }
    };

    public static final Predicate<GraphicObject> BOXED_GRAPHIC_OBJECT_PREDICATE = new Predicate<GraphicObject>() {
        @Override
        public boolean apply(GraphicObject graphicObject) {
            return graphicObject.getType() == GraphicObjectType.BITMAP || graphicObject.getType() == GraphicObjectType.VECTOR_BOX;
        }
    };
    protected StringBuilder caption = null;
    protected List<LayoutToken> captionLayoutTokens = new ArrayList<>();
    protected String labeledCaption = null;
    protected StringBuilder header = null;
    protected StringBuilder content = null;
    protected StringBuilder label = null;

    protected String id = null;
    protected URI uri = null;
    protected int start = -1; // start position in the full text tokenization
    protected int end = -1; // end position in the full text tokenization
    protected LayoutToken startToken = null; // start layout token
    protected LayoutToken endToken = null; // end layout token
    private List<BoundingBox> textArea;
    private List<LayoutToken> layoutTokens;

    // coordinates
    private int page = -1;
    private double y = 0.0;
    private double x = 0.0;
    private double width = 0.0;
    private double height = 0.0;

    // list of graphic objects corresponding to the figure
    protected List<GraphicObject> graphicObjects = null;
    private SortedSet<Integer> blockPtrs;

    public Figure() {
        caption = new StringBuilder();
        header = new StringBuilder();
        content = new StringBuilder();
        label = new StringBuilder();
    }

    public void appendHeader(String head) {
        header.append(head);
    }

    public String getHeader() {
        return header.toString();
    }

    public void appendCaption(String cap) {
        caption.append(cap);
    }

    public void appendCaptionLayoutTokens(List<LayoutToken> layoutTokens) {
        captionLayoutTokens.addAll(layoutTokens);
    }

    public String getCaption() {
        return caption.toString();
    }

    public List<LayoutToken> getCaptionLayoutTokens() {
        return this.captionLayoutTokens;
    }

    public void setCaptionLayoutTokens(List<LayoutToken> tokens) {
        this.captionLayoutTokens = tokens;
    }

    public void setLabeledCaption(String labeledCaption) {
        this.labeledCaption = labeledCaption;
    }

    public String getLabeledCaption() {
        return this.labeledCaption;
    }

    public void appendLabel(String lab) {
        label.append(lab);
    }

    public String getLabel() {
        return label.toString();
    }

    public void appendContent(String trash) {
        content.append(trash);
    }

    public String getContent() {
        return content.toString();
    }

    public void setURI(URI theURI) {
        uri = theURI;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStart() {
        return start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getEnd() {
        return end;
    }

    public void setStartToken(LayoutToken start) {
        this.startToken = start;
    }

    public LayoutToken getStartToken() {
        return startToken;
    }

    public void setEndToken(LayoutToken end) {
        this.endToken = end;
    }

    public LayoutToken getEndToken() {
        return endToken;
    }

    public void setId() {
        id = TextUtilities.cleanField(label.toString(), false);
    }

    public void setId(String theId) {
        id = theId;
    }

    public String getId() {
        return id;
    }

    public List<GraphicObject> getGraphicObjects() {
        return graphicObjects;
    }

    public List<GraphicObject> getBitmapGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists.newArrayList(Iterables.filter(this.graphicObjects, GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }

    public List<GraphicObject> getBoxedGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists.newArrayList(Iterables.filter(this.graphicObjects, BOXED_GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }

    public List<GraphicObject> getVectorBoxGraphicObjects() {
        if (graphicObjects == null) {
            return null;
        }
        ArrayList<GraphicObject> graphicObjects = Lists.newArrayList(Iterables.filter(this.graphicObjects, VECTOR_BOX_GRAPHIC_OBJECT_PREDICATE));
        if (graphicObjects.isEmpty()) {
            return null;
        }
        return graphicObjects;
    }


    public void addGraphicObject(GraphicObject obj) {
        if (graphicObjects == null)
            graphicObjects = new ArrayList<GraphicObject>();
        graphicObjects.add(obj);
    }

    public void setGraphicObjects(List<GraphicObject> objs) {
        graphicObjects = objs;
    }

    /**
     * Simple block coordinates. TBD: generate bounding box.
     */
    public String getCoordinates() {
        return String.format("%d,%.2f,%.2f,%.2f,%.2f", page, x, y, width, height);
    }

    public String toTEI(GrobidAnalysisConfig config, Document doc, TEIFormatter formatter) {
        if (StringUtils.isEmpty(header) && StringUtils.isEmpty(caption) && CollectionUtils.isEmpty(graphicObjects)) {
            return null;
        }
        Element figureElement = XmlBuilderUtils.teiElement("figure");
        if (id != null) {
            XmlBuilderUtils.addXmlId(figureElement, "fig_" + id);
        }

        if (config.isGenerateTeiCoordinates("figure")) {
            String coords;
            if (getBitmapGraphicObjects() != null && !getBitmapGraphicObjects().isEmpty()) {
                GraphicObject go = getBitmapGraphicObjects().get(0);
                coords = go.getBoundingBox().toString();
            } else {
                coords = LayoutTokensUtil.getCoordsString(getLayoutTokens());
            }

            if (coords != null) {
                XmlBuilderUtils.addCoords(figureElement, coords);
            }
        }
        if (header != null) {
            Element head = XmlBuilderUtils.teiElement("head",
                    LayoutTokensUtil.normalizeText(header.toString()));
            figureElement.appendChild(head);

        }
        if (label != null) {
            Element labelEl = XmlBuilderUtils.teiElement("label",
                LayoutTokensUtil.normalizeText(label.toString()));
            figureElement.appendChild(labelEl);
        }
        if (caption != null) {

            Element desc = XmlBuilderUtils.teiElement("figDesc");
            if (config.isGenerateTeiIds()) {
                String divID = KeyGen.getKey().substring(0, 7);
                addXmlId(desc, "_" + divID);
            }

            // if the segment has been parsed with the full text model we further extract the clusters
            // to get the bibliographical references
            if ( (labeledCaption != null) && (labeledCaption.length() > 0) ) {
                TaggingTokenClusteror clusteror = new TaggingTokenClusteror(GrobidModels.FULLTEXT, labeledCaption, captionLayoutTokens);
                List<TaggingTokenCluster> clusters = clusteror.cluster();
                
                for (TaggingTokenCluster cluster : clusters) {
                    if (cluster == null) {
                        continue;
                    }

                    TaggingLabel clusterLabel = cluster.getTaggingLabel();
                    String clusterContent = LayoutTokensUtil.normalizeText(cluster.concatTokens());
                    if (clusterLabel.equals(TaggingLabels.CITATION_MARKER)) {
                        try {
                            List<Node> refNodes = formatter.markReferencesTEILuceneBased(clusterContent,
                                    cluster.concatTokens(),
                                    doc.getReferenceMarkerMatcher(),
                                    config.isGenerateTeiCoordinates("ref"), 
                                    false);
                            if (refNodes != null) {
                                for (Node n : refNodes) {
                                    desc.appendChild(n);
                                }
                            }
                        } catch(Exception e) {
                            LOGGER.warn("Problem when serializing TEI fragment for figure caption", e);
                        }
                    } else {
                        desc.appendChild(textNode(clusterContent));
                    }
                }
            } else {
                desc.appendChild(LayoutTokensUtil.normalizeText(caption.toString()).trim());
                //Element desc = XmlBuilderUtils.teiElement("figDesc",
                //    LayoutTokensUtil.normalizeText(caption.toString()));
            }
            figureElement.appendChild(desc);
        }
        if ((graphicObjects != null) && (graphicObjects.size() > 0)) {
            for (GraphicObject graphicObject : graphicObjects) {
                Element go = XmlBuilderUtils.teiElement("graphic");
                String uri = graphicObject.getURI();
                if (uri != null) {
                    go.addAttribute(new Attribute("url", uri));
                }

                if (graphicObject.getBoundingBox() != null) {
                    go.addAttribute(new Attribute("coords", graphicObject.getBoundingBox().toString()));
                }

                go.addAttribute(new Attribute("type", graphicObject.getType().name().toLowerCase()));
                if (graphicObject.isMask()) {
                    go.addAttribute(new Attribute("mask", "true"));
                }
                figureElement.appendChild(go);
            }
        }
        return figureElement.toXML();
    }

    private String cleanString(String input) {
        return input.replace("\n", " ").replace("  ", " ").trim();
    }

    public int getPage() {
        return page;
    }

    public double getHeight() {
        return height;
    }

    public double getWidth() {
        return width;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public URI getUri() {
        return uri;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<BoundingBox> getTextArea() {
        return textArea;
    }

    public void setTextArea(List<BoundingBox> textArea) {
        this.textArea = textArea;
    }

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> layoutTokens) {
        this.layoutTokens = layoutTokens;
    }

    public void setBlockPtrs(SortedSet<Integer> blockPtrs) {
        this.blockPtrs = blockPtrs;
    }

    public SortedSet<Integer> getBlockPtrs() {
        return blockPtrs;
    }

    public void setCaption(StringBuilder caption) {
        this.caption = caption;
    }

    public void setHeader(StringBuilder header) {
        this.header = header;
    }

    public void setContent(StringBuilder content) {
        this.content = content;
    }

    public void setLabel(StringBuilder label) {
        this.label = label;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}
