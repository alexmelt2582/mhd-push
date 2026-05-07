package com.mhd.push.engine.deduplication;

import com.mhd.push.engine.deduplication.service.DeduplicationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhao-hao-dong
 */
@Service
public class DeduplicationHolder {
    private final Map<Integer, DeduplicationService> serviceHolder = new HashMap<>(4);

    public DeduplicationService selectService(Integer key) {
        return serviceHolder.get(key);
    }

    public void putService(Integer key, DeduplicationService service) {
        serviceHolder.put(key, service);
    }
}
