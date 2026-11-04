// 校园墙项目
// 启动时的数据初始化：如果校区表是空的，就先放一个广汉校区，方便本地第一次跑起来就有校区可选。
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
    // 启动时执行一次：如果校区表是空的，就先初始化一个校区。
    public void run(String... args) {
        if (campusRepository.count() == 0) {
            Campus campus = new Campus();
            campus.setName("广汉校区");
            campus.setCode("GUANGHAN");
            campusRepository.save(campus);
        }
    }
}
