package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.Employee;
import com.joker.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 实现员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        // 1、将页面提交的密码进行md5加密处理
        // 1.1、获取当前密码
        String password = employee.getPassword();
        // 1.2 实现加密操作
        password = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
        // 2、根据提交的用户名username查询数据库
        QueryWrapper<Employee> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username",employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);
        // 3、判断是否查询到该员工
        if (emp == null) {
            return R.error("抱歉，没有该用户信息");
        }
        // 4、进行密码比对
        if (!emp.getPassword().equals(password)){
            return R.error("密码不正确，请重试！");
        }
        // 5、查看当前登录员工的状态
        if (emp.getStatus() == 0){
            return R.error("该员工已被禁用！");
        }
        // 6、登录成功，存入员工id到session中
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 实现登出功能
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        return R.success("退出成功！");
    }

    /**
     * 添加员工
     * @param request
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> add(HttpServletRequest request, @RequestBody Employee employee){

        // 1、设置员工初始密码为123456，并进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8)));

        // 2、设置添加员工的时间和修改员工的时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());

        // 3、设置创建员工者和修改员工者id
//        Long createUserId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(createUserId);
//        employee.setUpdateUser(createUserId);

        // 4、保存到数据库中
        employeeService.save(employee);
        return R.success("添加员工成功");
    }

    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){

        log.info("执行修改员工信息");
        //1、获取当前修改者id
//        Long updateUserId = (Long) request.getSession().getAttribute("employee");

        // 2、更新修改的时间
//        employee.setUpdateUser(updateUserId);
//        employee.setUpdateTime(LocalDateTime.now());
        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);
        // 3、执行修改操作，并保存到数据库
        // 这里会出现精度损失问题 ===》具体看笔记
        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * 分页显示所有员工信息
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        log.info("page= {} , pageSize = {}, name = {}",page,pageSize,name);

        // 1、构造分页构造器
        Page<Employee> pageInfo = new Page<>(page,pageSize);

        // 2、构造Lambda条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 3、添加过滤条件，通过名称进行查询，如果为空，则不执行下面的条件，不为空才执行
        queryWrapper.like(StringUtils.isNotBlank(name),Employee::getName,name);

        // 4、添加排序条件 通过更新时间进行降序排序
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        // 5、执行查询
        employeeService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id查询员工信息
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        Employee employee = employeeService.getById(id);
        if (employee == null){
            R.error("查询失败！");
        }
        return R.success(employee);
    }

}
