package com.wjh.aicodegen.controller;

import com.mybatisflex.core.paginate.Page;
import com.wjh.aicodegen.annotation.AuthCheck;
import com.wjh.aicodegen.model.entity.VipCode;
import com.wjh.aicodegen.service.VipCodeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 *  控制层。
 *
 * @author 王哈哈
 * @since 2025-08-06 10:50:59
 */
@RestController
@RequestMapping("/vipCode")
public class VipCodeController {

    @Resource
    private VipCodeService vipCodeService;

    /**
     * 保存。
     *
     * @param vipCode 
     * @return {@code true} 保存成功，{@code false} 保存失败
     */
    @PostMapping("save")
    @AuthCheck(mustRole = "admin")
    public boolean save(@RequestBody VipCode vipCode) {
        return vipCodeService.save(vipCode);
    }

    /**
     * 根据主键删除。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @AuthCheck(mustRole = "admin")
    public boolean remove(@PathVariable Long id) {
        return vipCodeService.removeById(id);
    }

    /**
     * 根据主键更新。
     *
     * @param vipCode 
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @AuthCheck(mustRole = "admin")
    @PutMapping("update")
    public boolean update(@RequestBody VipCode vipCode) {
        return vipCodeService.updateById(vipCode);
    }

    /**
     * 查询所有。
     *
     * @return 所有数据
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("list")
    public List<VipCode> list() {
        return vipCodeService.list();
    }

    /**
     * 根据主键获取。
     *
     * @param id 主键
     * @return 详情
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("getInfo/{id}")
    public VipCode getInfo(@PathVariable Long id) {
        return vipCodeService.getById(id);
    }

    /**
     * 分页查询。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("page")
    public Page<VipCode> page(Page<VipCode> page) {
        return vipCodeService.page(page);
    }

}
