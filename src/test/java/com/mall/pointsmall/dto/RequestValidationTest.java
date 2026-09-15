package com.mall.pointsmall.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsZeroAndNegativeCheckoutQuantity() {
        OrderDtos.CheckoutRequest request = new OrderDtos.CheckoutRequest();
        request.setCheckoutToken("checkout_test_123456");
        request.setAddressId(1L);
        request.setExpectedTotalPoints(10);
        request.setExpectedBalanceBefore(100);
        OrderDtos.CheckoutItem item = new OrderDtos.CheckoutItem();
        item.setProductId(2L);
        item.setQuantity(0);
        request.setItems(List.of(item));

        assertFalse(validator.validate(request).isEmpty());

        item.setQuantity(-1);
        assertFalse(validator.validate(request).isEmpty());

        item.setQuantity(1);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsNegativeProductPriceAndStock() {
        AdminDtos.ProductSaveRequest request = validProduct();
        request.setPointsCost(-1);
        assertFalse(validator.validate(request).isEmpty());

        request = validProduct();
        request.setStock(-1);
        assertFalse(validator.validate(request).isEmpty());

        request = validProduct();
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void validatesShippingInformationLengthAndRequiredValues() {
        AdminDtos.ShipOrderRequest request = new AdminDtos.ShipOrderRequest();
        request.setShippingCompany("顺丰速运");
        request.setShippingNo("SF123456789");
        assertTrue(validator.validate(request).isEmpty());

        request.setShippingCompany(" ");
        assertFalse(validator.validate(request).isEmpty());

        request.setShippingCompany("物".repeat(101));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void validatesOptionalProductPurchaseLimits() {
        AdminDtos.ProductSaveRequest request = validProduct();
        request.setPerOrderLimit(null);
        request.setCustomerTotalLimit(null);
        assertTrue(validator.validate(request).isEmpty());

        request.setPerOrderLimit(1);
        request.setCustomerTotalLimit(999);
        assertTrue(validator.validate(request).isEmpty());

        request.setPerOrderLimit(0);
        assertFalse(validator.validate(request).isEmpty());

        request.setPerOrderLimit(1);
        request.setCustomerTotalLimit(1000);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidOrOversizedAddressFields() {
        OrderDtos.AddressRequest request = new OrderDtos.AddressRequest();
        request.setRecipientName("测试客户");
        request.setRecipientPhone("13155556666");
        request.setDetailAddress("测试地址");
        assertTrue(validator.validate(request).isEmpty());

        request.setRecipientPhone("abc");
        assertFalse(validator.validate(request).isEmpty());

        request.setRecipientPhone("13155556666");
        request.setRecipientName("姓".repeat(101));
        assertFalse(validator.validate(request).isEmpty());

        request.setRecipientName("测试客户");
        request.setDetailAddress("址".repeat(256));
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidCustomerAndAdminIdentityFields() {
        AdminDtos.CustomerCreateRequest customer = new AdminDtos.CustomerCreateRequest();
        customer.setName("测试客户");
        customer.setPhone("not-a-phone");
        customer.setIdCardNo("test-id");
        customer.setInitialPoints(0);
        assertFalse(validator.validate(customer).isEmpty());

        AdminDtos.AdminCreateRequest admin = new AdminDtos.AdminCreateRequest();
        admin.setName("测试员工");
        admin.setEmail("not-an-email");
        admin.setPassword("123456");
        assertFalse(validator.validate(admin).isEmpty());

        admin.setEmail("test@example.com");
        admin.setPassword("a".repeat(19));
        assertFalse(validator.validate(admin).isEmpty());
    }

    private AdminDtos.ProductSaveRequest validProduct() {
        AdminDtos.ProductSaveRequest request = new AdminDtos.ProductSaveRequest();
        request.setCategoryId(1L);
        request.setName("测试商品");
        request.setCoverImageUrl("");
        request.setGalleryJson("[]");
        request.setPointsCost(10);
        request.setStock(5);
        request.setEnabled(true);
        request.setSortOrder(0);
        request.setDescription("");
        return request;
    }
}
