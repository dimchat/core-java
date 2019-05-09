package chat.dim.protocols.file;

import chat.dim.dkd.Utils;
import chat.dim.protocols.ContentType;

import java.util.Map;

/**
 *  Video message: {
 *      type : 0x16,
 *      sn   : 123,
 *
 *      URL      : "http://", // upload to CDN
 *      data     : "...",     // if (!URL) base64_encode(video)
 *      snapshot : "...",     // base64_encode(smallImage)
 *      filename : "..."
 *  }
 */
public class VideoContent extends FileContent {

    private byte[] snapshot;

    public VideoContent(Map<String, Object> dictionary) {
        super(dictionary);
        String base64 = (String) dictionary.get("snapshot");
        if (base64 == null) {
            snapshot = null;
        } else {
            snapshot = Utils.base64Decode(base64);
        }
    }

    public VideoContent(byte[] data, String filename) {
        super(ContentType.VIDEO.value, data, filename);
        snapshot = null;
    }

    //-------- setter/getter --------

    public void setSnapshot(byte[] imageData) {
        snapshot = imageData;
        if (imageData == null) {
            dictionary.remove("snapshot");
        } else {
            dictionary.put("snapshot", Utils.base64Encode(imageData));
        }
    }

    public byte[] getSnapshot() {
        return snapshot;
    }
}
