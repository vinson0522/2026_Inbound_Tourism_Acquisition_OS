package org.dromara.project.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.project.domain.bo.CustomerProjectBo;
import org.dromara.project.domain.vo.CustomerProjectVo;
import org.dromara.project.service.ICustomerProjectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户项目 API — EPIC-1 FR-001
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/projects")
public class CustomerProjectController extends BaseController {

    private final ICustomerProjectService customerProjectService;

    @SaCheckLogin
    @GetMapping
    public TableDataInfo<CustomerProjectVo> list(CustomerProjectBo bo, PageQuery pageQuery) {
        return customerProjectService.queryPageList(bo, pageQuery);
    }

    /** 下拉选项：当前租户全部项目（不分页） */
    @SaCheckLogin
    @GetMapping("/options")
    public R<List<CustomerProjectVo>> options(CustomerProjectBo bo) {
        return R.ok(customerProjectService.queryList(bo));
    }

    @SaCheckLogin
    @GetMapping("/{id}")
    public R<CustomerProjectVo> getInfo(@NotNull @PathVariable Long id) {
        return R.ok(customerProjectService.queryById(id));
    }

    @SaCheckLogin
    @Log(title = "客户项目", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody CustomerProjectBo bo) {
        return R.ok(customerProjectService.insertByBo(bo));
    }

    @SaCheckLogin
    @Log(title = "客户项目", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public R<Void> edit(@NotNull @PathVariable Long id, @Validated(EditGroup.class) @RequestBody CustomerProjectBo bo) {
        bo.setId(id);
        return toAjax(customerProjectService.updateByBo(bo));
    }

    @SaCheckLogin
    @Log(title = "客户项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@NotNull @PathVariable Long id) {
        return toAjax(customerProjectService.deleteById(id));
    }
}
