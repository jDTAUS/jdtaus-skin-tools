/*
 *  Copyright (C) 2012 Christian Schulte
 *
 *  Permission to use, copy, modify, and/or distribute this software for any
 *  purpose with or without fee is hereby granted, provided that the above
 *  copyright notice and this permission notice appear in all copies.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 *  $JDTAUS$
 *
 */
package org.jdtaus.maven.skins.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.velocity.tools.config.DefaultKey;
import org.apache.velocity.tools.generic.SafeConfig;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Tool supporting rendering of language related markup.
 *
 * @author <a href="mailto:cs@schulte.it">Christian Schulte</a>
 * @version $JDTAUS$
 */
@DefaultKey( "LanguagesTool" )
public class LanguagesTool extends SafeConfig
{

    /** Creates a new {@code LanguagesTool} instance. */
    public LanguagesTool()
    {
        super();
    }

    /**
     * Creates HTML language navigation select element for a given page.
     * <p>The decoration model needs to specify language configuration elements like:
     * <pre><blockquote>
     * &lt;custom&gt;
     *   &lt;languages-tool languages-navigation=&quot;true&quot; default-language=&quot;en&quot;&gt;
     *     &lt;language&gt;de&lt;/language&gt;
     *     &lt;language&gt;en&lt;/language&gt;
     *     &lt;languages-navigation-exclude&gt;faq.html&lt;/languages-navigation-exclude&gt;
     *   &lt;/languages-tool&gt;
     * &lt;/custom&gt;
     * </blockquote></pre></p>
     *
     * @param model The decoration model to search for configuration elements.
     * @param locale The currently rendered locale.
     * @param relativePath The relative path to the currently rendered file.
     * @param currentFileName The file name of the currently rendered file.
     *
     * @return HTML language navigation links or an empty string, if {@code model} does not specify configuration
     * elements.
     *
     * @throws NullPointerException if {@code model}, {@code locale}, {@code relativePath} or {@code currentFileName}
     * is {@code null}.
     */
    public String languagesNavigationSelect( final DecorationModel model, final Locale locale,
                                             final String relativePath, final String currentFileName )
    {
        if ( model == null )
        {
            throw new NullPointerException( "model" );
        }
        if ( locale == null )
        {
            throw new NullPointerException( "locale" );
        }
        if ( relativePath == null )
        {
            throw new NullPointerException( "relativePath" );
        }
        if ( currentFileName == null )
        {
            throw new NullPointerException( "currentFileName" );
        }

        final StringBuilder selectBuilder = new StringBuilder( 255 );
        final LanguagesNavigationModel navigationModel = getLanguagesNavigationModel( model );

        if ( navigationModel != null
             && navigationModel.languagesNavigationEnabled
             && navigationModel.defaultLocale != null
             && !navigationModel.locales.isEmpty()
             && !navigationModel.languagesNavigationExcludes.contains( currentFileName ) )
        {
            selectBuilder.append( "<select class=\"langnav\" name=\"langnav\" size=\"1\" " ).
                append( "onchange=\"javascript:document.location=this.options[this.options.selectedIndex].value\">" );

            Collections.sort( navigationModel.locales, new Comparator<Locale>()
            {

                public int compare( final Locale o1, final Locale o2 )
                {
                    final String str1 = o1.getDisplayLanguage( o1 ) + " (" + o1.getDisplayLanguage( locale ) + ")";
                    final String str2 = o2.getDisplayLanguage( o2 ) + " (" + o2.getDisplayLanguage( locale ) + ")";
                    return str1.compareTo( str2 );
                }

            } );

            for ( final Locale targetLocale : navigationModel.locales )
            {
                final String displayLanguageHtml = StringEscapeUtils.escapeHtml(
                    targetLocale.getDisplayLanguage( targetLocale ) + " (" + targetLocale.getDisplayLanguage( locale )
                    + ")" );

                if ( !locale.equals( targetLocale ) )
                {
                    final String location;

                    if ( locale.equals( navigationModel.defaultLocale ) )
                    { // Rendering default locale - always link to sub-directory.
                        location = relativePath + "/" + targetLocale.getLanguage() + "/" + currentFileName;
                    }
                    else
                    { // Rendering non-default locale - link to base-directory or sub-directory.
                        location = targetLocale.equals( navigationModel.defaultLocale )
                                   ? relativePath + "/../" + currentFileName
                                   : relativePath + "/../" + targetLocale.getLanguage() + "/" + currentFileName;

                    }

                    selectBuilder.append( "<option value=\"" ).append( location ).append( "\">" ).
                        append( displayLanguageHtml ).append( "</option>" );

                }
                else
                {
                    selectBuilder.append( "<option value=\"" ).append( relativePath ).append( "/" ).
                        append( currentFileName ).append( "\" selected=\"selected\">" ).
                        append( displayLanguageHtml ).append( "</option>" );

                }
            }

            selectBuilder.append( "</select>" );
        }

        return selectBuilder.toString();
    }

    private static LanguagesNavigationModel getLanguagesNavigationModel( final DecorationModel decorationModel )
    {
        LanguagesNavigationModel navigationModel = null;

        if ( decorationModel.getCustom() instanceof Xpp3Dom )
        {
            final Xpp3Dom dom = (Xpp3Dom) decorationModel.getCustom();
            final Xpp3Dom languagesTool = dom.getChild( "languages-tool" );

            if ( languagesTool != null )
            {
                final String enabled = languagesTool.getAttribute( "languages-navigation" );
                final String defaultLanguage = languagesTool.getAttribute( "default-language" );

                if ( enabled != null && defaultLanguage != null )
                {
                    navigationModel = new LanguagesNavigationModel( Boolean.valueOf( enabled ), defaultLanguage );

                    final Xpp3Dom[] languages = languagesTool.getChildren( "language" );

                    if ( languages != null )
                    {
                        for ( final Xpp3Dom language : languages )
                        {
                            if ( language.getValue() != null )
                            {
                                navigationModel.locales.add( new Locale( language.getValue() ) );
                            }
                        }
                    }

                    final Xpp3Dom[] excludes = languagesTool.getChildren( "languages-navigation-exclude" );

                    if ( excludes != null )
                    {
                        for ( final Xpp3Dom exclude : excludes )
                        {
                            if ( exclude.getValue() != null )
                            {
                                navigationModel.languagesNavigationExcludes.add( exclude.getValue() );
                            }
                        }
                    }
                }
            }
        }

        return navigationModel;
    }

    private static class LanguagesNavigationModel
    {

        /** Flag indicating the languages navigation generator is enabled. */
        private final boolean languagesNavigationEnabled;

        /** The default language of the model. */
        private final Locale defaultLocale;

        /** The locales of the model. */
        private final List<Locale> locales = new ArrayList<Locale>();

        /** Set of file names to not generate languages navigation links for. */
        private final Set<String> languagesNavigationExcludes = new HashSet<String>();

        /** Creates a new {@code LanguagesModel} instance. */
        private LanguagesNavigationModel( final boolean languagesNavigationEnabled, final String defaultLanguage )
        {
            super();
            this.languagesNavigationEnabled = languagesNavigationEnabled;
            this.defaultLocale = defaultLanguage != null ? new Locale( defaultLanguage ) : null;
        }

    }

}
