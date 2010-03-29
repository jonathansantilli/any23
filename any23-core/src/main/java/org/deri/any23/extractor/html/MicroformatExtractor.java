/*
 * Copyright 2008-2010 Digital Enterprise Research Institute (DERI)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deri.any23.extractor.html;

import org.deri.any23.extractor.ExtractionException;
import org.deri.any23.extractor.ExtractionResult;
import org.deri.any23.extractor.Extractor.TagSoupDOMExtractor;
import org.deri.any23.extractor.ExtractorDescription;
import org.deri.any23.rdf.Any23ValueFactoryWrapper;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.w3c.dom.Document;

import java.io.IOException;

/**
 * The abstract base class for any
 * <a href="microformats.org/">Microformat specification</a> extractor.
 */
public abstract class MicroformatExtractor implements TagSoupDOMExtractor {

    public static final String BEGIN_SCRIPT = "<script>";
    public static final String END_SCRIPT   = "</script>";

    private HTMLDocument htmlDocument;

    private URI documentURI;

    private ExtractionResult out;

    protected final Any23ValueFactoryWrapper valueFactory =
            new Any23ValueFactoryWrapper(ValueFactoryImpl.getInstance());

    /**
     * Returns the description of this extractor.
     *
     * @return a human readable description.
     */
    public abstract ExtractorDescription getDescription();

    /**
     * Performs the extraction of the data and writes them to the model.
     * The nodes generated in the model can have any name or implicit label
     * but if possible they </i>SHOULD</i> have names (either URIs or AnonId) that
     * are uniquely derivable from their position in the DOM tree, so that
     * multiple extractors can merge information.
     */
    protected abstract boolean extract() throws ExtractionException;

    public HTMLDocument getHTMLDocument() {
        return htmlDocument;
    }

    public URI getDocumentURI() {
        return documentURI;
    }

    public final void run(Document in, URI documentURI, ExtractionResult out)
    throws IOException, ExtractionException {
        this.htmlDocument = new HTMLDocument(in);
        this.documentURI = documentURI;
        this.out = out;
        extract();
    }

    protected ExtractionResult openSubResult(Object context) {
        return out.openSubResult(context);
    }

    /**
     * Helper method that adds a literal property to a node.
     *
     * @return returns <code>true</code> if the value has been accepted and added, <code>false</code> otherwise.
     */
    protected boolean conditionallyAddStringProperty(Resource subject, URI p, String value) {
        if (value == null) return false;
        value = value.trim();
        return
                value.length() > 0 
                        &&
                conditionallyAddLiteralProperty(subject, p, valueFactory.createLiteral(value));
    }

    /**
     * Helper method that adds a literal property to a node.
     *
     * @param subject
     * @param property
     * @param literal
     * @return returns <code>true</code> if the literal has been accepted and added, <code>false</code> otherwise.
     */
    protected boolean conditionallyAddLiteralProperty(Resource subject, URI property, Literal literal) {
        final String literalStr = literal.stringValue();
        if( containsScriptBlock(literalStr) ) {
            out.notifyError(
                    ExtractionResult.ErrorLevel.WARN,
                    String.format("Detected script in literal: [%s]", literalStr)
                    ,-1
                    ,-1
            );
            return false;
        }
        out.writeTriple(subject, property, literal);
        return true;
    }

    /**
     * Helper method that adds a URI property to a node.
     */
    protected boolean conditionallyAddResourceProperty(Resource subject, URI property, URI uri) {
        if (uri == null) return false;
        out.writeTriple(subject, property, uri);
        return true;
    }

    /**
     * Helper method that adds a BNode property to a node.
     *
     * @param subject
     * @param property
     * @param bnode
     */
    protected void addBNodeProperty(Resource subject, URI property, BNode bnode) {
        out.writeTriple(subject, property, bnode);
    }

    /**
     * Helper method that adds a URI property to a node.
     *
     * @param subject
     * @param property
     * @param object
     */
    protected void addURIProperty(Resource subject, URI property, URI object) {
        out.writeTriple(subject, property, object);    
    }

    protected URI fixLink(String link) {
        return valueFactory.fixLink(link, null);
    }

    protected URI fixLink(String link, String defaultSchema) {
        return valueFactory.fixLink(link, defaultSchema);
    }

    private boolean containsScriptBlock(String in) {
        final String inLowerCase = in.toLowerCase();
        final int beginBlock = inLowerCase.indexOf(BEGIN_SCRIPT);
        if(beginBlock == -1) {
            return false;
        }
        return inLowerCase.indexOf(END_SCRIPT, beginBlock + BEGIN_SCRIPT.length()) != -1;
    }

}