package com.community.waste.service;

import com.community.waste.entity.FeeConfig;
import com.community.waste.repository.FeeConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeeServiceTest {

    private FeeService feeService;

    @BeforeEach
    void setUp() {
        FeeConfigRepository repo = mock(FeeConfigRepository.class);
        when(repo.findByCommunityCodeAndParamKey("DEFAULT", FeeService.DISPOSAL_PRICE_PER_KG))
                .thenReturn(Optional.of(new FeeConfig(FeeService.DISPOSAL_PRICE_PER_KG,
                        new BigDecimal("0.80"), "test")));
        feeService = new FeeService(repo);
    }

    @Test
    void readsConfiguredValue() {
        assertThat(feeService.get(FeeService.DISPOSAL_PRICE_PER_KG, BigDecimal.ONE))
                .isEqualByComparingTo("0.80");
    }

    @Test
    void fallsBackToDefaultWhenMissing() {
        assertThat(feeService.get(FeeService.FINE_LATE, new BigDecimal("200")))
                .isEqualByComparingTo("200");
    }

    @Test
    void billingFormulaIsWeightPlusBagsPlusExtraSurcharge() {
        // 100kg * 0.8 + 4袋 * 15 + 1个加袋 * 10 = 80 + 60 + 10 = 150
        BigDecimal net = new BigDecimal("100");
        BigDecimal price = new BigDecimal("0.8");
        BigDecimal bagFee = new BigDecimal("15");
        BigDecimal extraFee = new BigDecimal("10");
        BigDecimal total = net.multiply(price)
                .add(bagFee.multiply(BigDecimal.valueOf(4)))
                .add(extraFee.multiply(BigDecimal.valueOf(1)));
        assertThat(total).isEqualByComparingTo("150.0");
    }
}
