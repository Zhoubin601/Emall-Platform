package com.emall.backend.controller;

import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.SkuMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductControllerExportTest {

    @Test
    void exportProductsProducesAnExcelWorkbook() throws Exception {
        ProductMapper productMapper = mock(ProductMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        when(productMapper.selectList(isNull())).thenReturn(List.of());
        when(skuMapper.selectList(isNull())).thenReturn(List.of());

        ProductController controller = new ProductController();
        ReflectionTestUtils.setField(controller, "productMapper", productMapper);
        ReflectionTestUtils.setField(controller, "skuMapper", skuMapper);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.exportProducts(response);

        assertThat(response.getContentType())
                .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-disposition"))
                .contains("E-MALL%E5%85%A8%E9%87%8F%E5%95%86%E5%93%81%E4%B8%8E%E8%A7%84%E6%A0%BC%E6%80%BB%E8%A1%A8.xlsx");
        assertThat(response.getContentAsByteArray()).hasSizeGreaterThan(100);
    }
}
