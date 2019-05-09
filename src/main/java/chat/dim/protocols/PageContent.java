package chat.dim.protocols;

import chat.dim.dkd.Content;
import chat.dim.dkd.Utils;

import java.util.Map;

/**
 *  Web Page message: {
 *      type : 0x20,
 *      sn   : 123,
 *
 *      URL   : "https://github.com/moky/dimp", // Page URL
 *      icon  : "...",                          // base64_encode(icon)
 *      title : "...",
 *      desc  : "..."
 *  }
 */
public class PageContent extends Content {

    private String url;
    private String title;
    private String desc;
    private byte[] icon;

    public PageContent(Map<String, Object> dictionary) {
        super(dictionary);
        url   = (String) dictionary.get("URL");
        title = (String) dictionary.get("title");
        desc  = (String) dictionary.get("desc");
        String base64 = (String) dictionary.get("icon");
        if (base64 == null) {
            icon = null;
        } else {
            icon = Utils.base64Decode(base64);
        }
    }

    public PageContent(String url, String title, String desc, byte[] icon) {
        super(ContentType.PAGE.value);
        setURL(url);
        setTitle(title);
        setDesc(desc);
        setIcon(icon);
    }

    //-------- setters/getters --------

    public void setURL(String urlString) {
        url = urlString;
        dictionary.put("URL", urlString);
    }

    public String getUrl() {
        return url;
    }

    public void setTitle(String text) {
        title = text;
        dictionary.put("title", text);
    }

    public String getTitle() {
        return title;
    }

    public void setDesc(String text) {
        desc = text;
        dictionary.put("desc", text);
    }

    public String getDesc() {
        return desc;
    }

    public void setIcon(byte[] imageData) {
        icon = imageData;
        if (imageData == null) {
            dictionary.remove("icon");
        } else {
            dictionary.put("icon", Utils.base64Encode(imageData));
        }
    }

    public byte[] getIcon() {
        return icon;
    }
}
