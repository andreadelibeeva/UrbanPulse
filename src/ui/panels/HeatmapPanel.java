package ui.panels;

import model.CrimeRecord;
import ui.MainFrame;
import util.DataStore;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HeatmapPanel extends JPanel {

    private final DataStore dataStore;
    private MapCanvas canvas;
    private JComboBox<String> filterCombo;
    private JLabel statusLabel;

    public HeatmapPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
        refresh();
    }

    private void buildUI() {
        //Header
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setBackground(MainFrame.bgDark);

        JLabel title = new JLabel("Chicago Crime Map");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(MainFrame.textPrimary);

        JLabel sub = new JLabel("Scroll to zoom  ·  Drag to pan  ·  Hover a dot for details");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(MainFrame.textMuted);

        titleBlock.add(title);
        titleBlock.add(sub);
        header.add(titleBlock, BorderLayout.WEST);

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setBackground(MainFrame.bgDark);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setForeground(MainFrame.textMuted);
        filterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        filterCombo = new JComboBox<>(new String[]{"All Crime Types"});
        filterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterCombo.addActionListener(e -> canvas.repaint());

        JButton resetBtn = new JButton("Reset View");
        resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resetBtn.setBackground(new Color(0x2A3A5A));
        resetBtn.setForeground(MainFrame.textPrimary);
        resetBtn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        resetBtn.setFocusPainted(false);
        resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        resetBtn.addActionListener(e -> canvas.resetView());

        controls.add(filterLabel);
        controls.add(filterCombo);
        controls.add(Box.createHorizontalStrut(4));
        controls.add(resetBtn);
        header.add(controls, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        //MAPPPPP
        canvas = new MapCanvas();
        JPanel canvasWrap = new JPanel(new BorderLayout());
        canvasWrap.setBackground(MainFrame.bgDark);
        canvasWrap.setBorder(new EmptyBorder(0, 32, 0, 32));
        canvasWrap.add(canvas, BorderLayout.CENTER);
        add(canvasWrap, BorderLayout.CENTER);

        // status bar
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);
        statusLabel.setBorder(new EmptyBorder(8, 32, 16, 32));
        statusLabel.setBackground(MainFrame.bgDark);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void refresh() {
        String selected = (String) filterCombo.getSelectedItem();
        filterCombo.removeAllItems();
        filterCombo.addItem("All Crime Types");
        dataStore.getCrimeTypes().forEach(filterCombo::addItem);
        if (selected != null) filterCombo.setSelectedItem(selected);
        canvas.repaint();
    }

    private List<CrimeRecord> getFilteredCrimes() {
        String filter = (String) filterCombo.getSelectedItem();
        List<CrimeRecord> crimes = dataStore.getAllCrimes();
        if (filter != null && !filter.equals("All Crime Types")) {
            crimes = crimes.stream()
                    .filter(c -> c.getCrimeType().equals(filter))
                    .collect(Collectors.toList());
        }
        // Only return records that have real GPS coordinates
        return crimes.stream()
                .filter(c -> c.getLat() != 0.0 && c.getLon() != 0.0)
                .collect(Collectors.toList());
    }

    // map canvas supposedly

    private class MapCanvas extends JPanel {

        // Chicago city center
        private double centerLat = 41.8827;
        private double centerLon = -87.6233;
        private int zoom = 11;

        // OSM tile cache
        private final Map<String, BufferedImage> tileCache = new ConcurrentHashMap<>();
        private final Set<String> loading = Collections.synchronizedSet(new HashSet<>());
        private final ExecutorService tileLoader = Executors.newFixedThreadPool(6);

        // Pan drag state
        private Point dragStart;
        private double dragStartLat;
        private double dragStartLon;

        // Hover
        private CrimeRecord hoveredCrime = null;

        MapCanvas() {
            setBackground(new Color(0x18202E));
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            // zoom
            addMouseWheelListener(e -> {
                int notches = e.getWheelRotation();
                if (notches < 0 && zoom < 16) zoom++;
                else if (notches > 0 && zoom > 9) zoom--;
                repaint();
            });

            // pan
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragStart = e.getPoint();
                    dragStartLat = centerLat;
                    dragStartLon = centerLon;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
                @Override public void mouseReleased(MouseEvent e) {
                    dragStart = null;
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart == null) return;
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    double n = Math.pow(2, zoom);
                    double lonDelta = -dx * 360.0 / (n * 256.0);
                    double latRad   = Math.toRadians(dragStartLat);
                    double mercY    = Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad));
                    double newMercY = mercY + dy * 2.0 * Math.PI / (n * 256.0);
                    centerLat = Math.toDegrees(Math.atan(Math.sinh(newMercY)));
                    centerLon = dragStartLon + lonDelta;
                    repaint();
                }

                @Override public void mouseMoved(MouseEvent e) {
                    List<CrimeRecord> crimes = getFilteredCrimes();
                    CrimeRecord found = null;
                    for (CrimeRecord c : crimes) {
                        double[] px = latLonToScreen(c.getLat(), c.getLon());
                        if (Math.abs(px[0] - e.getX()) < 9 && Math.abs(px[1] - e.getY()) < 9) {
                            found = c;
                            break;
                        }
                    }
                    if (found != hoveredCrime) {
                        hoveredCrime = found;
                        if (found != null) {
                            statusLabel.setText("District " + found.getDistrict()
                                    + "  ·  " + found.getCrimeType()
                                    + ": " + found.getDescription()
                                    + "  ·  " + found.getDate());
                        } else {
                            statusLabel.setText(" ");
                        }
                        repaint();
                    }
                }
            });
        }

        void resetView() {
            centerLat = 41.8827;
            centerLon = -87.6233;
            zoom = 11;
            tileCache.clear();
            loading.clear();
            repaint();
        }


        private double fracTileX(double lon) {
            return (lon + 180.0) / 360.0 * Math.pow(2, zoom);
        }

        private double fracTileY(double lat) {
            double r = Math.toRadians(lat);
            return (1.0 - Math.log(Math.tan(r) + 1.0 / Math.cos(r)) / Math.PI)
                    / 2.0 * Math.pow(2, zoom);
        }

        /** lat/lon  →  panel pixel coordinates */
        private double[] latLonToScreen(double lat, double lon) {
            double cx = fracTileX(centerLon);
            double cy = fracTileY(centerLat);
            double sx = getWidth()  / 2.0 + (fracTileX(lon) - cx) * 256;
            double sy = getHeight() / 2.0 + (fracTileY(lat) - cy) * 256;
            return new double[]{sx, sy};
        }


        private BufferedImage getTile(int z, int x, int y) {
            String key = z + "/" + x + "/" + y;
            if (tileCache.containsKey(key)) return tileCache.get(key);
            if (loading.contains(key))      return null;
            loading.add(key);
            tileLoader.submit(() -> {
                try {
                    URL url = new URL("https://tile.openstreetmap.org/" + key + ".png");
                    URLConnection conn = url.openConnection();
                    conn.setRequestProperty("User-Agent", "UrbanPulse/1.0 (school-project)");
                    conn.setConnectTimeout(6_000);
                    conn.setReadTimeout(12_000);
                    BufferedImage img = ImageIO.read(conn.getInputStream());
                    tileCache.put(key, img != null ? img : blankTile());
                } catch (Exception ex) {
                    tileCache.put(key, blankTile());
                } finally {
                    loading.remove(key);
                    SwingUtilities.invokeLater(MapCanvas.this::repaint);
                }
            });
            return null;
        }

        private BufferedImage blankTile() {
            BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(0x1E2D40));
            g.fillRect(0, 0, 256, 256);
            g.dispose();
            return img;
        }


        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            double cx   = fracTileX(centerLon);
            double cy   = fracTileY(centerLat);
            int tileMax = (int) Math.pow(2, zoom) - 1;
            int xTiles  = (int) Math.ceil((double) getWidth()  / 256) + 2;
            int yTiles  = (int) Math.ceil((double) getHeight() / 256) + 2;
            int baseTX  = (int) Math.floor(cx) - xTiles / 2;
            int baseTY  = (int) Math.floor(cy) - yTiles / 2;

            for (int tx = baseTX; tx <= baseTX + xTiles; tx++) {
                for (int ty = baseTY; ty <= baseTY + yTiles; ty++) {
                    int clamped_x = Math.max(0, Math.min(tileMax, tx));
                    int clamped_y = Math.max(0, Math.min(tileMax, ty));
                    int sx = (int) (getWidth()  / 2.0 + (tx - cx) * 256);
                    int sy = (int) (getHeight() / 2.0 + (ty - cy) * 256);
                    BufferedImage tile = getTile(zoom, clamped_x, clamped_y);
                    if (tile != null) {
                        g2.drawImage(tile, sx, sy, 256, 256, null);
                    } else {
                        g2.setColor(new Color(0x1E2D40));
                        g2.fillRect(sx, sy, 256, 256);
                    }
                }
            }

            List<CrimeRecord> crimes = getFilteredCrimes();
            if (crimes.isEmpty()) {
                if (dataStore.getAllCrimes().isEmpty()) {
                    drawMessage(g2, "No data loaded — use Fetch Live Data or Load CSV File");
                } else {
                    drawMessage(g2, "No GPS data in loaded records — try Fetch Live Data for real coordinates");
                }
                drawZoomBadge(g2);
                g2.dispose();
                return;
            }

            drawHeatOverlay(g2, crimes);

            Map<String, Color> colorMap = buildColorMap(crimes);
            for (CrimeRecord c : crimes) {
                double[] px = latLonToScreen(c.getLat(), c.getLon());
                int sx = (int) px[0];
                int sy = (int) px[1];
                if (sx < -12 || sx > getWidth() + 12 || sy < -12 || sy > getHeight() + 12) continue;

                Color col = colorMap.getOrDefault(c.getCrimeType(), new Color(0xFF6B6B));
                boolean hov = (c == hoveredCrime);
                int r = hov ? 9 : 5;

                // Glow for hovered dot
                if (hov) {
                    g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60));
                    g2.fillOval(sx - r - 4, sy - r - 4, (r + 4) * 2, (r + 4) * 2);
                }
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), hov ? 230 : 170));
                g2.fillOval(sx - r, sy - r, r * 2, r * 2);
                if (hov) {
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(sx - r, sy - r, r * 2, r * 2);
                    drawTooltip(g2, c, sx, sy);
                }
            }

            drawLegend(g2, colorMap, crimes.size());

            drawZoomBadge(g2);

            g2.dispose();
        }


        private void drawHeatOverlay(Graphics2D g2, List<CrimeRecord> crimes) {
            int cell = Math.max(20, 50 - (zoom - 9) * 4);   // smaller cells at higher zoom
            int cols = getWidth()  / cell + 2;
            int rows = getHeight() / cell + 2;
            int[][] grid = new int[cols][rows];
            int maxCount = 1;

            for (CrimeRecord c : crimes) {
                double[] px = latLonToScreen(c.getLat(), c.getLon());
                int gx = (int)(px[0] / cell);
                int gy = (int)(px[1] / cell);
                if (gx >= 0 && gx < cols && gy >= 0 && gy < rows) {
                    grid[gx][gy]++;
                    if (grid[gx][gy] > maxCount) maxCount = grid[gx][gy];
                }
            }

            for (int gx = 0; gx < cols; gx++) {
                for (int gy = 0; gy < rows; gy++) {
                    if (grid[gx][gy] == 0) continue;
                    double ratio = (double) grid[gx][gy] / maxCount;
                    g2.setColor(heatColor(ratio));
                    g2.fillRect(gx * cell, gy * cell, cell, cell);
                }
            }
        }

        /** Returns a semi-transparent heat colour: cool blue → yellow → red */
        private Color heatColor(double t) {
            int r, g, b, a;
            if (t < 0.33) {
                double p = t / 0.33;
                r = (int)(20  + p * 235); g = (int)(80  - p * 80);  b = (int)(200 - p * 200);
            } else if (t < 0.66) {
                double p = (t - 0.33) / 0.33;
                r = 255; g = (int)(p * 200); b = 0;
            } else {
                double p = (t - 0.66) / 0.34;
                r = 255; g = (int)(200 - p * 200); b = 0;
            }
            a = (int)(60 + t * 130);   // more opaque where denser
            return new Color(
                    Math.min(255, r), Math.min(255, g), Math.min(255, b),
                    Math.min(255, a));
        }

        private Map<String, Color> buildColorMap(List<CrimeRecord> crimes) {
            Color[] palette = {
                    new Color(0xFF6B6B), new Color(0x4ECDC4), new Color(0xFFE66D),
                    new Color(0xA8E6CF), new Color(0xFF8B94), new Color(0xA29BFE),
                    new Color(0xFD79A8), new Color(0x55EFC4), new Color(0xFDAB00),
                    new Color(0x74B9FF), new Color(0xE17055), new Color(0x81ECEC)
            };
            Map<String, Color> map = new LinkedHashMap<>();
            int idx = 0;
            for (CrimeRecord c : crimes) {
                if (!map.containsKey(c.getCrimeType())) {
                    map.put(c.getCrimeType(), palette[idx % palette.length]);
                    idx++;
                }
            }
            return map;
        }

        private void drawTooltip(Graphics2D g2, CrimeRecord c, int sx, int sy) {
            String l1 = c.getCrimeType();
            String l2 = c.getDescription();
            String l3 = "District " + c.getDistrict() + "   ·   " + c.getDate();
            Font bold  = new Font("Segoe UI", Font.BOLD,  12);
            Font plain = new Font("Segoe UI", Font.PLAIN, 11);
            g2.setFont(bold);
            FontMetrics fm1 = g2.getFontMetrics();
            g2.setFont(plain);
            FontMetrics fm2 = g2.getFontMetrics();
            int tw = Math.max(fm1.stringWidth(l1), Math.max(fm2.stringWidth(l2), fm2.stringWidth(l3))) + 20;
            int th = fm1.getHeight() + fm2.getHeight() * 2 + 18;
            int tx = sx + 14;
            int ty = sy - th / 2;
            if (tx + tw > getWidth())  tx = sx - tw - 14;
            if (ty < 4)                ty = 4;
            if (ty + th > getHeight()) ty = getHeight() - th - 4;

            g2.setColor(new Color(10, 14, 30, 230));
            g2.fillRoundRect(tx, ty, tw, th, 10, 10);
            g2.setColor(new Color(80, 110, 210, 180));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(tx, ty, tw, th, 10, 10);

            int lx = tx + 10;
            g2.setFont(bold);
            g2.setColor(Color.WHITE);
            g2.drawString(l1, lx, ty + fm1.getAscent() + 6);
            g2.setFont(plain);
            g2.setColor(new Color(180, 190, 230));
            g2.drawString(l2, lx, ty + fm1.getHeight() + fm2.getAscent() + 6);
            g2.setColor(new Color(120, 135, 190));
            g2.drawString(l3, lx, ty + fm1.getHeight() + fm2.getHeight() + fm2.getAscent() + 6);
        }

        private void drawLegend(Graphics2D g2, Map<String, Color> colorMap, int total) {
            if (colorMap.isEmpty()) return;
            int maxShow = Math.min(colorMap.size(), 10);
            Font f  = new Font("Segoe UI", Font.PLAIN, 11);
            Font fb = new Font("Segoe UI", Font.BOLD,  11);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int lineH = 18;
            int padX = 10, padY = 8;
            int boxW = 0;
            int i = 0;
            for (String t : colorMap.keySet()) {
                if (i++ >= maxShow) break;
                boxW = Math.max(boxW, fm.stringWidth(t) + 22);
            }
            boxW += padX * 2;
            int extraLine = colorMap.size() > maxShow ? lineH : 0;
            int headerH = fm.getHeight() + 4;
            int boxH = headerH + maxShow * lineH + padY * 2 + extraLine;

            int lx = 12, ly = 12;
            g2.setColor(new Color(8, 12, 28, 215));
            g2.fillRoundRect(lx, ly, boxW, boxH, 10, 10);
            g2.setColor(new Color(60, 80, 160, 140));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(lx, ly, boxW, boxH, 10, 10);

            // Title
            g2.setFont(fb);
            g2.setColor(new Color(200, 210, 255));
            g2.drawString("Crime Types  (" + total + ")", lx + padX, ly + padY + fm.getAscent());

            // Entries
            g2.setFont(f);
            int ey = ly + padY + headerH + 4;
            i = 0;
            for (Map.Entry<String, Color> e : colorMap.entrySet()) {
                if (i >= maxShow) break;
                Color dot = e.getValue();
                g2.setColor(dot);
                g2.fillOval(lx + padX, ey + 3, 10, 10);
                g2.setColor(new Color(210, 215, 245));
                g2.drawString(e.getKey(), lx + padX + 14, ey + fm.getAscent());
                ey += lineH;
                i++;
            }
            if (colorMap.size() > maxShow) {
                g2.setColor(new Color(140, 150, 200));
                g2.drawString("+ " + (colorMap.size() - maxShow) + " more types", lx + padX + 14, ey + fm.getAscent());
            }
        }

        private void drawZoomBadge(Graphics2D g2) {
            String txt = "Zoom " + zoom;
            Font f = new Font("Segoe UI", Font.BOLD, 11);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(txt) + 16;
            int h = fm.getHeight() + 8;
            int x = getWidth()  - w - 10;
            int y = getHeight() - h - 10;
            g2.setColor(new Color(8, 12, 28, 200));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(new Color(180, 195, 255));
            g2.drawString(txt, x + 8, y + fm.getAscent() + 4);
        }

        private void drawMessage(Graphics2D g2, String msg) {
            Font f = new Font("Segoe UI", Font.PLAIN, 14);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(msg) + 32;
            int h = 40;
            int x = (getWidth()  - w) / 2;
            int y = (getHeight() - h) / 2;
            g2.setColor(new Color(8, 12, 28, 210));
            g2.fillRoundRect(x, y, w, h, 10, 10);
            g2.setColor(new Color(200, 210, 255));
            g2.drawString(msg, x + 16, y + fm.getAscent() + 10);
        }
    }
}