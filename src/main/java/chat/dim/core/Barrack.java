package chat.dim.core;

import chat.dim.crypto.PublicKey;
import chat.dim.mkm.entity.*;

public class Barrack implements IMetaDataSource, IEntityDataSource {

    private static Barrack ourInstance = new Barrack();

    public static Barrack getInstance() {
        return ourInstance;
    }

    private Barrack() {
    }

    public static PublicKey getPublicKey(ID identifier) {
        Meta meta = getInstance().getMeta(identifier);
        if (meta == null) {
            return null;
        } else {
            return meta.key;
        }
    }

    @Override
    public Meta getMeta(ID identifier) {
        return null;
    }

    @Override
    public Meta getMeta(Entity entity) {
        return getMeta(entity.identifier);
    }

    @Override
    public String getName(Entity entity) {
        return null;
    }
}
