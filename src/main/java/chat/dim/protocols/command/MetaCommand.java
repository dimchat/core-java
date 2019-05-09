package chat.dim.protocols.command;

import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocols.CommandContent;

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

    public MetaCommand(Map<String, Object> dictionary) throws ClassNotFoundException {
        super(dictionary);
        identifier = ID.getInstance(dictionary.get("ID"));
        meta       = Meta.getInstance(dictionary.get("meta"));
    }

    public MetaCommand(ID identifier, Meta meta) {
        super(META);
        // ID
        this.identifier = identifier;
        dictionary.put("ID", identifier);
        // meta
        this.meta = meta;
        if (meta != null) {
            dictionary.put("meta", meta);
        }
    }
}
