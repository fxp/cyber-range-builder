package com.cyberrange.pointsmall.config;

import com.cyberrange.pointsmall.model.Product;
import com.cyberrange.pointsmall.model.User;
import com.cyberrange.pointsmall.repository.ProductRepository;
import com.cyberrange.pointsmall.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        initUsers();
        initProducts();
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@cyberrange.com");
            admin.setPhone("13800000000");
            admin.setTotalPoints(10000L);
            admin.setAvailablePoints(10000L);
            admin.setLevel(User.UserLevel.GOLD);
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("testuser")) {
            User test = new User();
            test.setUsername("testuser");
            test.setPassword(passwordEncoder.encode("test123456"));
            test.setEmail("test@example.com");
            test.setPhone("13800000001");
            test.setTotalPoints(5000L);
            test.setAvailablePoints(5000L);
            test.setLevel(User.UserLevel.SILVER);
            userRepository.save(test);
        }
    }

    private void initProducts() {
        if (productRepository.count() > 0) return;

        Object[][] products = {
            {"星巴克咖啡券 中杯", "星巴克中杯拿铁兑换券，全国门店通用，有效期3个月", 500L, 1000, 256, "COUPON", "咖啡,饮品,星巴克"},
            {"京东E卡 50元", "京东购物电子卡，面值50元，全品类通用", 800L, 500, 123, "VIRTUAL", "购物卡,京东,电子卡"},
            {"运动水壶 500ml", "不锈钢真空保温运动水壶，500ml容量，颜色随机", 1200L, 200, 89, "PHYSICAL", "水壶,保温,运动"},
            {"有机绿茶礼盒 250g", "高山有机绿茶，礼盒装250g，产地云南", 2000L, 150, 45, "PHYSICAL", "茶叶,绿茶,礼盒,有机"},
            {"爱奇艺VIP月卡", "爱奇艺视频会员月卡，有效期30天", 300L, 2000, 567, "VIRTUAL", "视频,VIP,爱奇艺,会员"},
            {"植树公益证书", "参与一棵树的种植，获得专属公益证书", 100L, 9999, 1024, "CHARITY", "公益,环保,植树"},
            {"品牌蓝牙耳机", "无线蓝牙5.0耳机，续航20小时，支持主动降噪", 15000L, 50, 12, "PHYSICAL", "耳机,蓝牙,数码"},
            {"下午茶体验券", "五星级酒店双人下午茶体验，含茶饮和点心", 3000L, 100, 34, "EXPERIENCE", "下午茶,酒店,体验"},
            {"网易云音乐黑胶VIP月卡", "网易云音乐黑胶VIP会员月卡，畅听无损音质", 350L, 3000, 789, "VIRTUAL", "音乐,VIP,网易云"},
            {"定制马克杯", "陶瓷马克杯，可印制个人照片或文字，容量350ml", 600L, 300, 156, "PHYSICAL", "马克杯,定制,礼品"},
        };

        for (Object[] p : products) {
            Product product = new Product();
            product.setName((String) p[0]);
            product.setDescription((String) p[1]);
            product.setPointsPrice((Long) p[2]);
            product.setStock((Integer) p[3]);
            product.setSoldCount((Integer) p[4]);
            product.setCategory(Product.ProductCategory.valueOf((String) p[5]));
            product.setTags((String) p[6]);
            product.setStatus(Product.ProductStatus.ON_SALE);
            productRepository.save(product);
        }
    }
}
