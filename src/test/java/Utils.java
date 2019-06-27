import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static byte[] readResourceFile(String filename) throws IOException {
        InputStream is = Utils.class.getResourceAsStream(filename);
        assert is != null;
        int len = is.available();
        byte[] data = new byte[len];
        is.read(data, 0, len);
        return data;
    }

    public static String readTextFile(String filename) throws IOException {
        byte[] data = readResourceFile(filename);
        return new String(data, "UTF-8");
    }

    public static String hexEncode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        String hex;
        for (byte ch : data) {
            hex = Integer.toHexString(ch & 0xFF);
            sb.append(hex.length() == 1 ? "0" + hex : hex);
        }
        return sb.toString();
    }
}
