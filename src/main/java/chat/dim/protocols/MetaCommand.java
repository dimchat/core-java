package chat.dim.protocols;

import chat.dim.dkd.content.CommandContent;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command : "meta", // command name
 *      ID      : "{ID}", // contact's ID
 *      meta    : {...}   // When meta is empty, means query meta for ID
 *  }
 */
public class MetaCommand extends CommandContent {

    public final ID identifier;
    public final Meta meta;

    public MetaCommand(MetaCommand content) {
        super(content);
        this.identifier = content.identifier;
        this.meta       = content.meta;
    }

    public MetaCommand(Map<String, Object> dictionary) throws UnsupportedEncodingException, ClassNotFoundException {
        super(dictionary);
        this.identifier = ID.getInstance(dictionary.get("ID"));
        this.meta       = Meta.getInstance(dictionary.get("meta"));
    }

    public MetaCommand(ID identifier, Meta meta) {
        super(META);
        // ID
        this.identifier = identifier;
        this.dictionary.put("ID", identifier.toString());
        // meta
        this.meta = meta;
        if (meta != null) {
            this.dictionary.put("meta", meta);
        }
    }
}
