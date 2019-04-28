package chat.dim.core;

import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.ToStringSerializer;

import java.util.Map;

public class JsON {

    public static String encode(Object container) {
        return JSON.toJSONString(container);
    }

    public static Map<String, Object> decode(String jsonString) {
        return JSON.parseObject(jsonString);
    }


    static {
        SerializeConfig serializeConfig = SerializeConfig.getGlobalInstance();
        serializeConfig.put(ID.class, ToStringSerializer.instance);
        serializeConfig.put(Address.class, ToStringSerializer.instance);
    }
}
