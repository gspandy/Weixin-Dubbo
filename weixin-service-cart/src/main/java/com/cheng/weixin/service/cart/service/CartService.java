package com.cheng.weixin.service.cart.service;

import com.cheng.weixin.rpc.cart.service.RpcCartService;
import com.cheng.weixin.rpc.redis.service.RpcRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Desc: 购物车
 * Author: cheng
 * Date: 2016/6/28
 */
@Service("cartService")
public class CartService implements RpcCartService {

    private static final String CART = "CART_";

    @Autowired
    private RpcRedisService redisService;

    /**
     * 增加商品的数量
     *
     * @param productId
     * @param count
     */
    @Override
    public void addProductCount(String userId, String productId, Integer count) {
        boolean exists = redisService.exists(getCart(userId), productId);
        if (exists) {
            redisService.add(getCart(userId), productId, String.valueOf(count));
        }else {
            redisService.add(getCart(userId), productId, null);
        }
    }

    /**
     * 获取购物车中所有商品的ID
     *
     * @param userId
     * @return
     */
    @Override
    public Set<String> getProductIds(String userId) {
        return redisService.getFields(getCart(userId));
    }

    /**
     * 获取当前用户购物车的标识
     * @param userId
     * @return
     */
    private String getCart(String userId) {
        return CART+userId;
    }
}
