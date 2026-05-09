package com.cookiemusic.controller;

import com.cookiemusic.entity.po.ProductInfo;
import com.cookiemusic.entity.query.ProductInfoQuery;
import com.cookiemusic.entity.vo.ResponseVO;
import com.cookiemusic.service.ProductInfoService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/product")
@Slf4j
public class ProductInfoController extends ABaseController {

    @Resource
    private ProductInfoService productInfoService;

    @RequestMapping("/loadProduct")
    public ResponseVO loadProduct() {
        ProductInfoQuery productInfoQuery = new ProductInfoQuery();
        productInfoQuery.setOrderBy("p.sort asc");
        List<ProductInfo> productInfoList = productInfoService.findListByParam(productInfoQuery);
        return getSuccessResponseVO(productInfoList);
    }

    @RequestMapping("/saveProduct")
    public ResponseVO saveProduct(MultipartFile coverFile, ProductInfo productInfo) {
        productInfoService.saveProduct(coverFile, productInfo);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/changeProductSort")
    public ResponseVO changeProductSort(String productIds) {
        productInfoService.changeProductSort(productIds);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/delProduct")
    public ResponseVO delProduct(String productId) {
        productInfoService.deleteProductInfoByProductId(productId);
        return getSuccessResponseVO(null);
    }
}
