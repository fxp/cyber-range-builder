-- 初始化管理员账户（密码: admin123，BCrypt加密）
INSERT INTO users (username, password, email, phone, total_points, available_points, frozen_points, status, level, created_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6ZiKe', 'admin@cyberrange.com', '13800000000', 10000, 10000, 0, 'ACTIVE', 'GOLD', NOW());

-- 初始化测试用户（密码: test123456）
INSERT INTO users (username, password, email, phone, total_points, available_points, frozen_points, status, level, created_at)
VALUES ('testuser', '$2a$10$slYQmyNdgTY18LfY.UdCr.jZ68e28pGHpEhFJyUJQzL3T.HklJ9fK', 'test@example.com', '13800000001', 5000, 5000, 0, 'ACTIVE', 'SILVER', NOW());

-- 初始化商品数据
INSERT INTO products (name, description, points_price, stock, sold_count, category, status, tags, created_at)
VALUES
('星巴克咖啡券 中杯', '星巴克中杯拿铁兑换券，全国门店通用，有效期3个月', 500, 1000, 256, 'COUPON', 'ON_SALE', '咖啡,饮品,星巴克', NOW()),
('京东E卡 50元', '京东购物电子卡，面值50元，全品类通用', 800, 500, 123, 'VIRTUAL', 'ON_SALE', '购物卡,京东,电子卡', NOW()),
('运动水壶 500ml', '不锈钢真空保温运动水壶，500ml容量，颜色随机', 1200, 200, 89, 'PHYSICAL', 'ON_SALE', '水壶,保温,运动', NOW()),
('有机绿茶礼盒 250g', '高山有机绿茶，礼盒装250g，产地云南', 2000, 150, 45, 'PHYSICAL', 'ON_SALE', '茶叶,绿茶,礼盒,有机', NOW()),
('爱奇艺VIP月卡', '爱奇艺视频会员月卡，有效期30天', 300, 2000, 567, 'VIRTUAL', 'ON_SALE', '视频,VIP,爱奇艺,会员', NOW()),
('植树公益证书', '参与一棵树的种植，获得专属公益证书', 100, 9999, 1024, 'CHARITY', 'ON_SALE', '公益,环保,植树', NOW()),
('品牌蓝牙耳机', '无线蓝牙5.0耳机，续航20小时，支持主动降噪', 15000, 50, 12, 'PHYSICAL', 'ON_SALE', '耳机,蓝牙,数码', NOW()),
('下午茶体验券', '五星级酒店双人下午茶体验，含茶饮和点心', 3000, 100, 34, 'EXPERIENCE', 'ON_SALE', '下午茶,酒店,体验', NOW()),
('网易云音乐黑胶VIP月卡', '网易云音乐黑胶VIP会员月卡，畅听无损音质', 350, 3000, 789, 'VIRTUAL', 'ON_SALE', '音乐,VIP,网易云', NOW()),
('定制马克杯', '陶瓷马克杯，可印制个人照片或文字，容量350ml', 600, 300, 156, 'PHYSICAL', 'ON_SALE', '马克杯,定制,礼品', NOW());
