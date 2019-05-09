package chat.dim.protocols.file;

import chat.dim.dkd.Content;
import chat.dim.protocols.ContentType;

import java.util.Map;

/**
 *  File message: {
 *      type : 0x10,
 *      sn   : 123,
 *
 *      URL      : "http://", // upload to CDN
 *      data     : "...",     // if (!URL) base64_encode(fileContent)
 *      filename : "..."
 *  }
 */
public class FileContent extends Content {

    private String url;
    private byte[] data; // file data (plaintext)
    private String filename;

    private Map<String, Object> password; // symmetric key to decrypt the encrypted data from URL

    @SuppressWarnings("unchecked")
    public FileContent(Map<String, Object> dictionary) {
        super(dictionary);
        url = (String) dictionary.get("URL");
        data = null; // NOTICE: file data should not exists here
        filename = (String) dictionary.get("filename");
        password = (Map<String, Object>) dictionary.get("password");
    }

    protected FileContent(int type, byte[] data, String filename) {
        super(type);
        setUrl(null);
        setData(data);
        setFilename(filename);
        setPassword(null);
    }

    public FileContent(byte[] data, String filename) {
        this(ContentType.FILE.value, data, filename);
    }

    //-------- setters/getters --------

    public void setUrl(String urlString) {
        url = urlString;
        if (urlString == null) {
            dictionary.remove("URL");
        } else {
            dictionary.put("URL", urlString);
        }
    }

    public String getUrl() {
        return url;
    }

    public void setData(byte[] fileData) {
        data = fileData;
        // NOTICE: do not set file data in dictionary, which will be post onto the DIM network
    }

    public byte[] getData() {
        return data;
    }

    public void setFilename(String name) {
        filename = name;
        if (name == null) {
            dictionary.remove("filename");
        } else {
            dictionary.put("filename", name);
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setPassword(Map<String, Object> key) {
        password = key;
        if (key == null) {
            dictionary.remove("password");
        } else {
            dictionary.put("password", key);
        }
    }

    public Map<String, Object> getPassword() {
        return password;
    }
}
