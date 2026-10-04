package com.community.waste.service;

import com.community.waste.entity.FeeConfig;
import com.community.waste.repository.FeeConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** 计费参数服务：按小区读取，缺失时使用内置默认值。 */
@Service
public class FeeService {

    private static final Logger log = LoggerFactory.getLogger(FeeService.class);

    public static final String DISPOSAL_PRICE_PER_KG = "disposal.price.per.kg";
    public static final String BAG_FEE = "bag.fee";
    public static final String EXTRA_BAG_SURCHARGE = "extra.bag.surcharge";
    public static final String COMPANY_SETTLE_PER_KG = "company.settle.per.kg";
    public static final String FINE_FIRE_LANE = "fine.fire.lane";
    public static final String FINE_MIXED = "fine.mixed.household";
    public static final String FINE_ILLEGAL = "fine.illegal.location";
    public static final String FINE_LATE = "fine.late";
    public static final String FINE_SPILL = "fine.spill";
    public static final String ENCLOSURE_CAPACITY_FACTOR = "enclosure.capacity.factor";
    public static final String WEIGH_DEVIATION_LIMIT = "weigh.deviation.limit";
    public static final String RECEIPT_DIFF_LIMIT = "receipt.diff.limit";
    public static final String DEFAULT_BAG_KG = "bag.estimated.kg";

    private final FeeConfigRepository repository;

    public FeeService(FeeConfigRepository repository) {
        this.repository = repository;
    }

    public BigDecimal get(String communityCode, String key, BigDecimal defaultValue) {
        return repository.findByCommunityCodeAndParamKey(communityCode, key)
                .map(FeeConfig::getParamValue)
                .or(() -> repository.findByCommunityCodeAndParamKey("DEFAULT", key)
                        .map(FeeConfig::getParamValue))
                .orElse(defaultValue);
    }

    public BigDecimal get(String key, BigDecimal defaultValue) {
        return get("DEFAULT", key, defaultValue);
    }

    @Transactional
    public FeeConfig set(String key, BigDecimal value, String remark) {
        FeeConfig cfg = repository.findByCommunityCodeAndParamKey("DEFAULT", key).orElseGet(() -> {
            FeeConfig c = new FeeConfig();
            c.setCommunityCode("DEFAULT");
            c.setParamKey(key);
            return c;
        });
        cfg.setParamValue(value);
        if (remark != null) cfg.setRemark(remark);
        return repository.save(cfg);
    }

    public List<FeeConfig> all() {
        return repository.findAll();
    }
}
