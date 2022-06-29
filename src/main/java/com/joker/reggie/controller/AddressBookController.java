package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.joker.reggie.common.BaseContext;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.AddressBook;
import com.joker.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBook
     * @return
     */
    @PostMapping
    public R<AddressBook> add(@RequestBody AddressBook addressBook){
        // 1、获取当前用户id
        Long userId = BaseContext.getCurrentId();
        // 2、将userId保存到对应的地址簿中
        addressBook.setUserId(userId);
        // 3、保存到数据中
        boolean save = addressBookService.save(addressBook);
        if (!save){
            R.error("保存地址失败！");
        }
        return R.success(addressBook);
    }

    /**
     * 显示当前用户设置的所有地址
     * @param addressBook
     * @return
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook){
        // 1、获取当前用户id
        addressBook.setUserId(BaseContext.getCurrentId());
        // 2、根据userId查询所有地址
        LambdaQueryWrapper<AddressBook> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(null != addressBook.getUserId(),AddressBook::getUserId,addressBook.getUserId())
                .eq(AddressBook::getIsDeleted,0);
        List<AddressBook> addressBookList = addressBookService.list(lambdaQueryWrapper);
        if (addressBookList == null){
            return R.error("目前没添加任何地址");
        }
        return R.success(addressBookList);
    }

    /**
     * 设置默认地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @Transactional
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook){
        // 1、将当前用户的所设地址默认去除
        LambdaUpdateWrapper<AddressBook> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.eq(AddressBook::getUserId,BaseContext.getCurrentId()).set(AddressBook::getIsDefault,0);
        boolean update = addressBookService.update(lambdaUpdateWrapper);
        if (!update){
            return R.error("修改失败！");
        }

        // 2、然后将新设置为默认地址的is_default字段修改为1
        addressBook.setIsDefault(1);
        boolean b = addressBookService.updateById(addressBook);
        if (!b){
            return R.error("修改失败！");
        }

        return R.success(addressBook);
    }

    /**
     * 根据id查询地址信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<AddressBook> getById(@PathVariable Long id){
        // 根据id查询地址信息
        AddressBook addressBook = addressBookService.getOne(new LambdaQueryWrapper<AddressBook>().eq(AddressBook::getId, id));
        // 判断
        if (addressBook == null) {
            return R.error("抱歉该地址信息不存在");
        }
        return R.success(addressBook);
    }

    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    public R<AddressBook> getDefault(){
        // 1、获取当前登录userId
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<AddressBook> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 2、条件：当前用户的默认地址
        lambdaQueryWrapper.eq(AddressBook::getUserId,userId).eq(AddressBook::getIsDefault,1);
        // 3、查询
        AddressBook addressBook = addressBookService.getOne(lambdaQueryWrapper);
        if (addressBook == null){
            return R.error("请先设置默认地址！");
        }
        return R.success(addressBook);
    }

    /**
     * 修改地址信息
     * @param addressBook
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody AddressBook addressBook){
        boolean b = addressBookService.updateById(addressBook);
        if (!b){
            R.error("修改失败！");
        }
        return R.success("修改地址成功！");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        // 1、通过id查询所有的地址信息
        List<AddressBook> addressBookList = addressBookService.listByIds(ids);
        if (addressBookList == null) {
            R.error("抱歉，没有相应地址信息");
        }
        for (AddressBook addressBook : addressBookList) {
            // 批量进行逻辑删除
            addressBook.setIsDeleted(1);
            // 如果是默认地址的话，就取消默认的地址
            if (addressBook.getIsDefault() == 1){
                addressBook.setIsDefault(0);
            }
            addressBookService.updateById(addressBook);
        }
        return R.success("删除成功！");
    }
}
