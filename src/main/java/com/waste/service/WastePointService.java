package com.waste.service;

import com.waste.auth.Session;
import com.waste.common.BizException;
import com.waste.common.TraceLogger;
import com.waste.entity.WastePoint;
import com.waste.repository.WastePointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WastePointService {

    private final WastePointRepository wastePointRepo;
    private final TraceLogger traceLogger;

    public List<WastePoint> list() {
        return wastePointRepo.findAll();
    }

    @Transactional
    public WastePoint create(Session property, String name, String location, int capacityBags) {
        WastePoint p = new WastePoint();
        p.setName(name);
        p.setLocation(location);
        p.setCapacityBags(capacityBags);
        wastePointRepo.save(p);
        traceLogger.record(null, "WASTE_POINT", p.getId(), property, "CREATE_WASTE_POINT",
                "新增垃圾点: " + name + "，容量 " + capacityBags + " 袋");
        return p;
    }

    /** 物业安排/撤除临时围挡（扩容后接近上限的预约可通过） */
    @Transactional
    public WastePoint setFence(Session property, Long id, boolean enable, int extraCapacity) {
        WastePoint p = wastePointRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "垃圾点不存在"));
        if (enable && extraCapacity <= 0) {
            throw new BizException("启用临时围挡时必须指定新增容量");
        }
        p.setTempFence(enable);
        p.setTempExtraCapacity(enable ? extraCapacity : 0);
        wastePointRepo.save(p);
        traceLogger.record(null, "WASTE_POINT", p.getId(), property,
                enable ? "ENABLE_TEMP_FENCE" : "DISABLE_TEMP_FENCE",
                (enable ? "安排临时围挡，扩容 +" + extraCapacity + " 袋，有效容量 " + p.effectiveCapacity()
                        : "撤除临时围挡") + "（当前已堆放 " + p.getCurrentBags() + " 袋）");
        return p;
    }
}
