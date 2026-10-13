package Tunnel;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

            // Determine base dimensions from viewBox or doc.size()
            float baseW = (doc.viewBox() != null && doc.viewBox().width > 0) ? doc.viewBox().width : doc.size().width;
            float baseH = (doc.viewBox() != null && doc.viewBox().height > 0) ? doc.viewBox().height : doc.size().height;
            if (baseW <= 0) baseW = 100;
            if (baseH <= 0) baseH = 100;

            // Target high resolution for cave surveying background sketches:
            // Ensure the longest dimension is at least 3500px so details, text and lines are crisp.
            // If the SVG already specifies higher resolution, keep it up to 6000px.
            final float TARGET_RES = 3500.0f;
            final float MAX_RES = 6000.0f;
            float maxDim = Math.max(baseW, baseH);

            float scale = 1.0f;
            if (maxDim < TARGET_RES) {
                scale = TARGET_RES / maxDim;
            } else if (maxDim > MAX_RES) {
                scale = MAX_RES / maxDim;
            }

            int targetW = Math.max(1, Math.round(baseW * scale));
            int targetH = Math.max(1, Math.round(baseH * scale));

            BufferedImage img = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                doc.render(null, g, new ViewBox(0, 0, targetW, targetH));
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
