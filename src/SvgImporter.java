package Tunnel;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URL;

/**
 * Utility for importing SVG files as BufferedImages using the JSVG library.
 * This class avoids any usage of Apache Batik as per project constraints.
 */
public class SvgImporter {
    /**
     * Loads an SVG represented by a {@link FileAbstraction} and renders it to a {@link BufferedImage}.
     *
     * @param fa FileAbstraction pointing to an SVG file (local file or URL).
     * @return BufferedImage containing the rendered SVG, or {@code null} on failure.
     */
    public static BufferedImage loadSvg(FileAbstraction fa) {
        try {
            URL svgUrl = (fa.localurl != null) ? fa.localurl : fa.localfile.toURI().toURL();
            SVGLoader loader = new SVGLoader();
            SVGDocument doc = loader.load(svgUrl);
            if (doc == null) {
                TN.emitWarning("Failed to load SVG document from " + svgUrl);
                return null;
            }
            float w = doc.size().width;
            float h = doc.size().height;
            if (w <= 0) w = 100;
            if (h <= 0) h = 100;
            BufferedImage img = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                doc.render(null, g, null);
            } finally {
                g.dispose();
            }
            return img;
        } catch (Exception e) {
            TN.emitWarning("SvgImporter.loadSvg error: " + e.getMessage());
            return null;
        }
    }
}
