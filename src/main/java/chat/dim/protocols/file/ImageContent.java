package chat.dim.protocols.file;

import chat.dim.dkd.Utils;
import chat.dim.protocols.ContentType;

import java.util.Map;

/**
 *  Image message: {
 *      type : 0x12,
 *      sn   : 123,
 *
 *      URL       : "http://", // upload to CDN
 *      data      : "...",     // if (!URL) base64_encode(image)
 *      thumbnail : "...",     // base64_encode(smallImage)
 *      filename  : "..."
 *  }
 */
public class ImageContent extends FileContent {

    private byte[] thumbnail;

    public ImageContent(Map<String, Object> dictionary) {
        super(dictionary);
        String base64 = (String) dictionary.get("thumbnail");
        if (base64 == null) {
            thumbnail = null;
        } else {
            thumbnail = Utils.base64Decode(base64);
        }
    }

    public ImageContent(byte[] data, String filename) {
        super(ContentType.IMAGE.value, data, filename);
        thumbnail = null;
    }

    //-------- setter/getter --------

    public void setThumbnail(byte[] imageData) {
        thumbnail = imageData;
        if (imageData == null) {
            dictionary.remove("thumbnail");
        } else {
            dictionary.put("thumbnail", Utils.base64Encode(imageData));
        }
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }
}
