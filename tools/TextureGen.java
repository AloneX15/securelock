import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Genera las texturas de Secure Mod (pixel art procedural original, 16x16).
 * Uso: java tools/TextureGen.java src/main/resources/assets/securemod
 */
public class TextureGen {
    // Paleta
    static final int STEEL_DARK = 0xFF2B2F35, STEEL = 0xFF454B53, STEEL_LIGHT = 0xFF6B737D, STEEL_HI = 0xFF9AA3AD;
    static final int RIVET = 0xFFC9D1DA, OAK_DARK = 0xFF6B4F2C, OAK = 0xFF8F6B3E, OAK_LIGHT = 0xFFB08A55;
    static final int BLACK = 0xFF121417, GREEN = 0xFF3DDC84, GREEN_DARK = 0xFF1E7A47, RED = 0xFFE5484D, RED_DARK = 0xFF7A1E22;
    static final int AMBER = 0xFFFFB224, CYAN = 0xFF4CC9F0, CYAN_DARK = 0xFF1B6F8A, GOLD = 0xFFE8C547, PURPLE = 0xFFB66DFF;
    static final int TRANSPARENT = 0x00000000;

    static File root;

    public static void main(String[] args) throws IOException {
        root = new File(args.length > 0 ? args[0] : "src/main/resources/assets/securemod");
        new File(root, "textures/block").mkdirs();
        new File(root, "textures/item").mkdirs();

        // Bloques
        save("block/reinforced_iron_door_top", ironDoor(true));
        save("block/reinforced_iron_door_bottom", ironDoor(false));
        save("block/reinforced_oak_door_top", oakDoor(true));
        save("block/reinforced_oak_door_bottom", oakDoor(false));
        save("block/reinforced_iron_trapdoor", ironTrapdoor());
        save("block/reinforced_oak_planks", reinforcedPlanks());
        save("block/passcode_barrel_side", barrelSide());
        save("block/passcode_barrel_top", barrelTop(false));
        save("block/passcode_barrel_top_open", barrelTop(true));
        save("block/passcode_barrel_bottom", barrelBottom());
        save("block/security_panel_side", panelSide());
        save("block/keypad", keypad(false));
        save("block/keypad_on", keypad(true));
        save("block/card_reader", cardReader(false));
        save("block/card_reader_on", cardReader(true));
        save("block/biometric_scanner", scanner(false));
        save("block/biometric_scanner_on", scanner(true));

        // Ítems
        save("item/reinforced_iron_door", doorItem(false));
        save("item/reinforced_oak_door", doorItem(true));
        save("item/padlock", padlock());
        save("item/universal_modifier", wand(CYAN, CYAN_DARK));
        save("item/universal_block_remover", wand(RED, RED_DARK));
        save("item/admin_tool", wand(PURPLE, 0xFF5B2A8A));
        save("item/card_writer", cardWriter());
        int[] cardColors = {0xFF9AA3AD, GREEN, CYAN, AMBER, PURPLE};
        for (int i = 0; i < 5; i++) {
            save("item/keycard_lv" + (i + 1), keycard(cardColors[i], i + 1));
        }

        // Icono del mod (128x128): escudo con cerradura
        ImageIO.write(icon(), "png", new File(root, "icon.png"));
        System.out.println("Texturas generadas en " + root);
    }

    // ---------------- Utilidades ----------------

    static BufferedImage img() {
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    static void fill(BufferedImage g, int x0, int y0, int x1, int y1, int c) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) set(g, x, y, c);
    }

    static void set(BufferedImage g, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < g.getWidth() && y < g.getHeight()) g.setRGB(x, y, c);
    }

    static void border(BufferedImage g, int x0, int y0, int x1, int y1, int light, int dark) {
        for (int x = x0; x <= x1; x++) { set(g, x, y0, light); set(g, x, y1, dark); }
        for (int y = y0; y <= y1; y++) { set(g, x0, y, light); set(g, x1, y, dark); }
    }

    static int shade(int c, double f) {
        int a = c >>> 24, r = (c >> 16) & 255, gr = (c >> 8) & 255, b = c & 255;
        r = clamp((int) (r * f)); gr = clamp((int) (gr * f)); b = clamp((int) (b * f));
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    static void noise(BufferedImage g, int x0, int y0, int x1, int y1, long seed, double amount) {
        Random r = new Random(seed);
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            int c = g.getRGB(x, y);
            if ((c >>> 24) == 0) continue;
            g.setRGB(x, y, shade(c, 1.0 + (r.nextDouble() - 0.5) * amount));
        }
    }

    static void rivet(BufferedImage g, int x, int y) {
        set(g, x, y, RIVET);
        set(g, x + 1, y + 1, STEEL_DARK);
    }

    static void save(String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(root, "textures/" + name + ".png"));
    }

    // ---------------- Bloques ----------------

    static BufferedImage steelPlate(long seed) {
        BufferedImage g = img();
        fill(g, 0, 0, 15, 15, STEEL);
        noise(g, 0, 0, 15, 15, seed, 0.12);
        border(g, 0, 0, 15, 15, STEEL_LIGHT, STEEL_DARK);
        return g;
    }

    static BufferedImage ironDoor(boolean top) {
        BufferedImage g = steelPlate(top ? 11 : 12);
        // Paneles con bisel
        border(g, 2, 2, 13, 13, STEEL_DARK, STEEL_LIGHT);
        fill(g, 3, 3, 12, 12, shade(STEEL, 0.92));
        noise(g, 3, 3, 12, 12, top ? 21 : 22, 0.1);
        // Banda de refuerzo
        int band = top ? 12 : 3;
        fill(g, 1, band, 14, band + 1, STEEL_HI);
        fill(g, 1, band + 1, 14, band + 1, STEEL_LIGHT);
        rivet(g, 2, band); rivet(g, 7, band); rivet(g, 12, band);
        if (top) {
            // Mirilla
            fill(g, 6, 4, 9, 6, BLACK);
            border(g, 5, 3, 10, 7, STEEL_DARK, STEEL_HI);
        } else {
            // Cerradura
            fill(g, 11, 7, 12, 9, BLACK);
            set(g, 11, 6, STEEL_HI); set(g, 12, 6, STEEL_HI);
            set(g, 11, 10, AMBER);
        }
        return g;
    }

    static BufferedImage oakDoor(boolean top) {
        BufferedImage g = img();
        // Tablones verticales
        for (int x = 0; x < 16; x++) {
            int c = (x % 4 == 0) ? OAK_DARK : (x % 4 == 2 ? OAK_LIGHT : OAK);
            fill(g, x, 0, x, 15, c);
        }
        noise(g, 0, 0, 15, 15, top ? 31 : 32, 0.15);
        border(g, 0, 0, 15, 15, OAK_LIGHT, OAK_DARK);
        // Bandas de acero
        int[] bands = top ? new int[]{2, 10} : new int[]{4, 12};
        for (int b : bands) {
            fill(g, 0, b, 15, b + 1, STEEL);
            fill(g, 0, b, 15, b, STEEL_LIGHT);
            rivet(g, 2, b); rivet(g, 13, b);
        }
        if (top) {
            fill(g, 6, 5, 9, 7, BLACK);
            border(g, 5, 4, 10, 8, STEEL_DARK, STEEL_LIGHT);
        } else {
            fill(g, 11, 7, 12, 9, STEEL_DARK);
            set(g, 11, 8, BLACK); set(g, 12, 8, BLACK);
        }
        return g;
    }

    static BufferedImage ironTrapdoor() {
        BufferedImage g = steelPlate(41);
        // Rejilla de refuerzo
        for (int i = 3; i <= 12; i += 3) {
            fill(g, i, 1, i, 14, STEEL_DARK);
            fill(g, 1, i, 14, i, STEEL_DARK);
        }
        rivet(g, 1, 1); rivet(g, 13, 1); rivet(g, 1, 13); rivet(g, 13, 13);
        fill(g, 7, 7, 8, 8, AMBER);
        return g;
    }

    static BufferedImage reinforcedPlanks() {
        BufferedImage g = img();
        for (int y = 0; y < 16; y++) {
            int c = (y % 4 == 3) ? OAK_DARK : (y % 4 == 0 ? OAK_LIGHT : OAK);
            fill(g, 0, y, 15, y, c);
        }
        noise(g, 0, 0, 15, 15, 51, 0.15);
        // Bandas metálicas verticales
        fill(g, 3, 0, 4, 15, STEEL);
        fill(g, 11, 0, 12, 15, STEEL);
        fill(g, 3, 0, 3, 15, STEEL_LIGHT);
        fill(g, 11, 0, 11, 15, STEEL_LIGHT);
        for (int y = 1; y < 16; y += 4) { rivet(g, 3, y); rivet(g, 11, y); }
        return g;
    }

    static BufferedImage barrelSide() {
        BufferedImage g = img();
        for (int x = 0; x < 16; x++) {
            int c = (x % 4 == 0) ? STEEL_DARK : (x % 4 == 2 ? STEEL_LIGHT : STEEL);
            fill(g, x, 0, x, 15, c);
        }
        noise(g, 0, 0, 15, 15, 61, 0.1);
        for (int b : new int[]{2, 13}) {
            fill(g, 0, b, 15, b, STEEL_HI);
            fill(g, 0, b + 1, 15, b + 1, STEEL_DARK);
        }
        fill(g, 6, 7, 9, 8, AMBER);
        return g;
    }

    static BufferedImage barrelTop(boolean open) {
        BufferedImage g = steelPlate(open ? 71 : 72);
        border(g, 2, 2, 13, 13, STEEL_DARK, STEEL_HI);
        if (open) {
            fill(g, 3, 3, 12, 12, BLACK);
        } else {
            fill(g, 3, 3, 12, 12, shade(STEEL, 0.9));
            // Mini teclado
            for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) set(g, 5 + x * 2, 5 + y * 2, STEEL_HI);
            set(g, 7, 11, GREEN);
        }
        return g;
    }

    static BufferedImage barrelBottom() {
        BufferedImage g = steelPlate(81);
        border(g, 2, 2, 13, 13, STEEL_DARK, STEEL_LIGHT);
        rivet(g, 3, 3); rivet(g, 11, 3); rivet(g, 3, 11); rivet(g, 11, 11);
        return g;
    }

    static BufferedImage panelSide() {
        BufferedImage g = img();
        fill(g, 0, 0, 15, 15, STEEL_DARK);
        noise(g, 0, 0, 15, 15, 91, 0.1);
        return g;
    }

    /** Frente de un panel (la zona visible del modelo es x 3..12, y 2..13). */
    static BufferedImage panelFront(long seed) {
        BufferedImage g = img();
        fill(g, 0, 0, 15, 15, STEEL_DARK);
        fill(g, 3, 2, 12, 13, STEEL);
        noise(g, 3, 2, 12, 13, seed, 0.1);
        border(g, 3, 2, 12, 13, STEEL_LIGHT, BLACK);
        return g;
    }

    static BufferedImage keypad(boolean on) {
        BufferedImage g = panelFront(101);
        // Pantalla
        fill(g, 4, 3, 11, 4, on ? GREEN_DARK : BLACK);
        if (on) { set(g, 5, 3, GREEN); set(g, 7, 3, GREEN); set(g, 9, 3, GREEN); }
        // Teclas 3x4
        for (int row = 0; row < 4; row++) for (int col = 0; col < 3; col++) {
            int x = 5 + col * 2, y = 6 + row * 2;
            set(g, x, y, STEEL_HI);
        }
        set(g, 11, 12, on ? GREEN : RED_DARK);
        return g;
    }

    static BufferedImage cardReader(boolean on) {
        BufferedImage g = panelFront(111);
        // Ranura
        fill(g, 5, 5, 10, 5, BLACK);
        fill(g, 5, 6, 10, 6, STEEL_DARK);
        // Tarjeta parcialmente insertada
        fill(g, 6, 3, 9, 4, on ? CYAN : STEEL_LIGHT);
        // LEDs
        set(g, 5, 10, on ? GREEN : GREEN_DARK);
        set(g, 7, 10, AMBER);
        set(g, 9, 10, on ? RED_DARK : RED);
        return g;
    }

    static BufferedImage scanner(boolean on) {
        BufferedImage g = panelFront(121);
        int lens = on ? CYAN : CYAN_DARK;
        // Lente
        fill(g, 6, 5, 9, 8, BLACK);
        fill(g, 6, 6, 9, 7, lens);
        set(g, 7, 6, on ? 0xFFE0FBFF : CYAN);
        border(g, 5, 4, 10, 9, STEEL_HI, STEEL_DARK);
        // Línea de escaneo
        fill(g, 4, 11, 11, 11, on ? GREEN : STEEL_DARK);
        return g;
    }

    // ---------------- Ítems ----------------

    static BufferedImage doorItem(boolean oak) {
        BufferedImage g = img();
        int base = oak ? OAK : STEEL, dark = oak ? OAK_DARK : STEEL_DARK, light = oak ? OAK_LIGHT : STEEL_LIGHT;
        fill(g, 4, 0, 11, 15, base);
        noise(g, 4, 0, 11, 15, oak ? 131 : 132, 0.12);
        border(g, 4, 0, 11, 15, light, dark);
        fill(g, 4, 4, 11, 4, oak ? STEEL : STEEL_HI);
        fill(g, 4, 11, 11, 11, oak ? STEEL : STEEL_HI);
        fill(g, 6, 2, 9, 2, BLACK);
        set(g, 10, 8, AMBER);
        return g;
    }

    static BufferedImage padlock() {
        BufferedImage g = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Arco
        for (int y = 2; y <= 7; y++) { set(g, 5, y, STEEL_HI); set(g, 10, y, STEEL_LIGHT); }
        fill(g, 6, 1, 9, 1, STEEL_HI);
        set(g, 5, 2, STEEL_LIGHT); set(g, 10, 2, STEEL_LIGHT);
        // Cuerpo
        fill(g, 3, 7, 12, 14, GOLD);
        noise(g, 3, 7, 12, 14, 141, 0.1);
        border(g, 3, 7, 12, 14, 0xFFFFE08A, 0xFF9A7A1E);
        fill(g, 7, 9, 8, 10, BLACK);
        set(g, 7, 11, BLACK); set(g, 8, 11, BLACK); set(g, 7, 12, BLACK);
        return g;
    }

    static BufferedImage wand(int tip, int tipDark) {
        BufferedImage g = img();
        // Mango diagonal
        for (int i = 0; i < 9; i++) {
            set(g, 2 + i, 13 - i, STEEL_LIGHT);
            set(g, 3 + i, 13 - i, STEEL_DARK);
        }
        set(g, 2, 13, BLACK); set(g, 3, 13, BLACK); set(g, 2, 14, BLACK);
        // Cabezal
        fill(g, 10, 2, 13, 5, tipDark);
        fill(g, 11, 2, 13, 4, tip);
        set(g, 12, 3, 0xFFFFFFFF);
        border(g, 10, 2, 13, 5, tip, tipDark);
        return g;
    }

    static BufferedImage keycard(int stripe, int level) {
        BufferedImage g = img();
        fill(g, 1, 4, 14, 12, 0xFFE8EBEF);
        border(g, 1, 4, 14, 12, 0xFFFFFFFF, 0xFF9AA3AD);
        fill(g, 1, 5, 14, 6, stripe);
        // Chip
        fill(g, 3, 8, 5, 10, GOLD);
        set(g, 4, 9, 0xFF9A7A1E);
        // Puntos de nivel
        for (int i = 0; i < level; i++) set(g, 8 + i, 10, stripe);
        return g;
    }

    static BufferedImage cardWriter() {
        BufferedImage g = img();
        fill(g, 1, 6, 14, 14, STEEL);
        noise(g, 1, 6, 14, 14, 151, 0.1);
        border(g, 1, 6, 14, 14, STEEL_LIGHT, STEEL_DARK);
        fill(g, 4, 7, 11, 7, BLACK);
        // Tarjeta asomando
        fill(g, 5, 2, 10, 6, 0xFFE8EBEF);
        fill(g, 5, 3, 10, 3, CYAN);
        set(g, 3, 11, GREEN); set(g, 5, 11, AMBER);
        fill(g, 8, 10, 12, 12, BLACK);
        set(g, 9, 11, GREEN);
        return g;
    }

    static BufferedImage icon() {
        int size = 128;
        BufferedImage g = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double cx = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Escudo: rectángulo arriba que se estrecha hacia una punta abajo
                double nx = (x - cx) / (size * 0.40);
                double ny = (y - size * 0.10) / (size * 0.82);
                boolean inside = ny >= 0 && ny <= 1 && Math.abs(nx) <= (ny < 0.55 ? 1.0 : Math.sqrt(Math.max(0, 1 - Math.pow((ny - 0.55) / 0.45, 2))));
                if (!inside) continue;
                boolean edge = ny < 0.04 || Math.abs(nx) > (ny < 0.55 ? 0.9 : 0.9 * Math.sqrt(Math.max(0, 1 - Math.pow((ny - 0.55) / 0.45, 2)))) || ny > 0.95;
                int c = edge ? STEEL_HI : (nx < 0 ? STEEL : STEEL_DARK);
                g.setRGB(x, y, c);
            }
        }
        // Cerradura dorada en el centro
        int bx = 44, by = 58, bw = 40, bh = 30;
        for (int y = by; y < by + bh; y++) for (int x = bx; x < bx + bw; x++) g.setRGB(x, y, GOLD);
        for (int t = 0; t < 6; t++) {
            for (int a = 0; a <= 180; a++) {
                double rad = Math.toRadians(a);
                int x = (int) Math.round(64 + Math.cos(rad) * (14 - t * 0.5 + 0));
                int y = (int) Math.round(58 - Math.sin(rad) * (18 - t * 0.5));
                if (x >= 0 && y >= 0 && x < size && y < size) g.setRGB(x, y, STEEL_HI);
            }
        }
        for (int y = 66; y < 76; y++) for (int x = 61; x < 67; x++) g.setRGB(x, y, BLACK);
        for (int y = 76; y < 82; y++) for (int x = 62; x < 66; x++) g.setRGB(x, y, BLACK);
        return g;
    }
}
