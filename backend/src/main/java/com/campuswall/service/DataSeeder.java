// 校园墙项目
// 启动时的数据初始化：如果校区表是空的，就先放两个初始校区，方便本地第一次跑起来就有校区可选和订阅演示。
// 初始校区列表只是起点，实际校区以学校官方为准，可以在数据库里增删改，不用改代码。
package com.campuswall.service;

import com.campuswall.entity.Campus;
import com.campuswall.repository.CampusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final CampusRepository campusRepository;

    @Override
    // 启动时执行一次：如果校区表是空的，就先初始化初始校区。
    public void run(String... args) {
        if (campusRepository.count() == 0) {
            Campus guanghan = new Campus();
            guanghan.setName("广汉校区");
            guanghan.setCode("GUANGHAN");
            campusRepository.save(guanghan);
            Campus tianfu = new Campus();
            tianfu.setName("天府校区");
            tianfu.setCode("TIANFU");
            campusRepository.save(tianfu);
        }
    }
}
