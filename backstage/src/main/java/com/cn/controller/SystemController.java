package com.cn.controller;

import com.alibaba.fastjson.JSON;
import com.cn.LogService;
import com.cn.ManagerService;
import com.cn.RoleService;
import com.cn.config.ManagerDetail;
import com.cn.entity.LoginLog;
import com.cn.entity.Manager;
import com.cn.entity.Permission;
import com.cn.entity.Role;
import com.cn.util.MenuUtil;
import com.cn.util.RestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.*;
import java.security.Principal;
import java.util.*;

/**
 * 后台客服管理员 控制类
 *
 * @author chen
 * @date 2018-01-02 18:25
 */
@Controller
@RequestMapping("/sys")
public class SystemController {
    @Autowired
    ManagerService managerService;
    @Autowired
    RoleService roleService;
    @Autowired
    LogService logService;

    /***
     * 下面两个底层实现一样  唯一区别就是hasRole默认添加 ROLE_ 前缀
     * @PreAuthorize("hasAuthority('ROLE_admin')")
     * @PreAuthorize("hasRole('admin')") 方法调用前判断是否有权限
     * @PreAuthorize("hasPermission('','sys:user')") 判断自定义权限标识符
     * @PostAuthorize("returnObject.id%2==0") 方法调用完后判断 若为false则无权限  基本用不上
     * @PostFilter("filterObject.id%2==0") 对返回结果进行过滤  filterObject内置为返回对象
     * @PreFilter(filterTarget="ids", value="filterObject%2==0") 对方法参数进行过滤 如有多参则指定参数 ids为其中一个参数
     */

    /**
     * 管理员列表页
     */
    @RequestMapping("/adminList")
    public String managerList(@PageableDefault(sort = { "createTime" }, direction = Sort.Direction.DESC)
                                      Pageable pageable, Model model, @Valid Manager manager){
        Page<Manager> managerList = managerService.getManagersList(pageable,manager);
        model.addAttribute("managerList",managerList);
        model.addAttribute("manger",manager);
        return "manager/managerList";
    }

    /**
     * 管理员详情页
     */
    @RequestMapping("/adminView")
    public String managerDetail(@RequestParam String mangerId, Model model){
        Manager manager = managerService.getManagerById(mangerId);
        model.addAttribute("manager",manager);
        return "manager/managerDetail";
    }

    /**
     * 管理员编辑页
     */
    @PreAuthorize("hasAuthority('admin')")
    @RequestMapping("/adminEdit")
    public String altManager(@RequestParam(required = false)String managerId, Principal principal,Model model){
        //最好是从当前授权信息里面提出角色列表来
        Authentication authentication = (Authentication) principal;
        ManagerDetail managerDetail = (ManagerDetail) authentication.getPrincipal();
        Set<Role> roles = managerDetail.getRoleList();
        if("admin".equals(managerDetail.getUsername())){
            roles = roleService.getAvailableRoles();
        }
        List<String> optrole = new ArrayList<>();
        roles.stream().forEach(role -> optrole.add(role.getId().toString()));
        Manager manager = new Manager();
        ArrayList<String> checkRoleIds = new ArrayList<>();
        if(managerId!=null){
            manager = managerService.getManagerById(managerId);
            for(Role role:manager.getRoleList()){
                checkRoleIds.add(role.getId().toString());
                roles.add(role);
            }
        }
        model.addAttribute("manager",manager);
        //待选角色列表
        model.addAttribute("roles",roles);
        //已勾选角色ID
        model.addAttribute("checkRoleId",String.join(",",checkRoleIds));
        //可授权角色ID
        model.addAttribute("optionRoles",String.join(",",optrole));
        return "manager/managerEdit";
    }

    /**
     * 新增或修改管理员信息
     */
    @PreAuthorize("hasAuthority('admin')")
    @ResponseBody
    @RequestMapping("/adminSave")
    public ModelMap register(@Valid Manager manager){
        ManagerDetail managerDetail = (ManagerDetail) SecurityContextHolder.getContext().getAuthentication() .getPrincipal();
        try {
            return managerService.saveManager(managerDetail,manager);
        }catch (Exception e){
            return RestUtil.Error(500);
        }
    }

    /**
     * 删除管理员
     */
    @PreAuthorize("hasAuthority('admin')")
    @ResponseBody
    @RequestMapping("/adminDel")
    public ModelMap delManager(@RequestParam String managerId){
        try {
            managerService.delManager(managerId);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 修改密码页面
     * @return
     */
    @RequestMapping("/altPwd")
    public String altPwd(){
        return "manager/updatePwd";
    }

    /**
     * 修改密码
     */
    @RequestMapping("/updatePwd")
    @ResponseBody
    public ModelMap updatePassword(@RequestParam String oldPassword,@RequestParam String password){
        ManagerDetail managerDetail = (ManagerDetail) SecurityContextHolder.getContext().getAuthentication() .getPrincipal();
        try {
            return managerService.updatePassword(managerDetail.getId(),oldPassword,password);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
    }

    /**
     * 重置管理员密码
     * @param managerId
     * @return
     */
//    @PreAuthorize("hasAuthority('admin')")
    @ResponseBody
    @RequestMapping("/resetPwd")
    public ModelMap resetPassword(@RequestParam String managerId){
        try {
            managerService.updatePassword(managerId,"123456");
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 角色列表页
     */
    @RequestMapping("/roleList")
    public String roleList(@PageableDefault(sort = { "roleName" }, direction = Sort.Direction.DESC)
                                   Pageable pageable,@Valid Role role,Model model){
        Page<Role> roleList = roleService.getRoleList(pageable,role);
        model.addAttribute("roleList",roleList);
        model.addAttribute("roleRt",role);
        return "role/roleList";
    }

    /**
     * 新增或修改角色页面
     */
    @RequestMapping("/roleEdit")
    public String roleEdit(@RequestParam(required = false) Long roleId,Model model){
        Role role = new Role();
        if(roleId!=null){
            role = roleService.findRole(roleId);
        }
        model.addAttribute("role",role);
        return "role/roleEdit";
    }

    /**
     * 保存角色
     */
    @RequestMapping("/roleSave")
    @ResponseBody
    public ModelMap saveRole(@Valid Role role){
        try {
            roleService.saveRole(role);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500,"服务异常");
        }
        return RestUtil.Success();
    }

    /**
     * 角色授权页面
     */
    @RequestMapping("/grant")
    public String grantForm(@RequestParam long roleId, Model model) {
        List<Permission> permissions = roleService.getPermissionList();
        List<Permission> rolePermissions = roleService.findRole(roleId).getPermissions();
        String menuList = JSON.toJSONString(MenuUtil.makeTreeList(permissions,rolePermissions));
        model.addAttribute("roleId", roleId);
        model.addAttribute("menuList",menuList);
        return "role/grant";
    }

    /**
     * 保存授权
     * @param roleId
     * @param menuIds
     * @return
     */
    @PostMapping("/saveGrant")
    @ResponseBody
    public ModelMap saveGrant(long roleId, String menuIds){
        try {
            roleService.saveGrant(roleId,menuIds);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 是否停用角色
     */
    @RequestMapping("/togAvailable")
    @ResponseBody
    public ModelMap altRole(@RequestParam long roleId){
        try {
            roleService.altAvailable(roleId);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500,"服务异常");
        }
        return RestUtil.Success();
    }

    /**
     * 删除角色
     */
    @PreAuthorize("hasAuthority('admin')")
    @RequestMapping("/roleDel")
    @ResponseBody
    public ModelMap delRole(@RequestParam long roleId){
        try {
            roleService.delRole(roleId);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 菜单列表页
     */
    @RequestMapping("/menuList")
    public String menuList(Model model){
        List<Permission> permissionList = roleService.getPermissionList();
        model.addAttribute("menuList",MenuUtil.treeOrderList(permissionList));
        return "menu/menuList";
    }

    /**
     * 新增或修改菜单页
     */
    @RequestMapping("/menuEdit")
    public String menuEdit(@RequestParam(required = false)Long menuId,@RequestParam(required = false)Long parentId,Model model){
        Permission permission = new Permission();
        String parentName="";
        if(parentId!=null){
            parentName = roleService.getPermissionById(parentId).getName();
            permission.setParentId(parentId);
        }
        if(menuId!=null){
            permission = roleService.getPermissionById(menuId);
            parentName = roleService.getPermissionById(permission.getParentId()).getName();
        }
        model.addAttribute("menu",permission);
        model.addAttribute("parentName",parentName);
        return "menu/menuEdit";
    }

    /**
     * 保存菜单
     */
    @RequestMapping("/menuSave")
    @ResponseBody
    public ModelMap saveMenu(@Valid Permission permission){
        try {
            roleService.saveMenu(permission);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 删除菜单
     */
    @PreAuthorize("hasAuthority('admin')")
    @RequestMapping("/menuDel")
    @ResponseBody
    public ModelMap delMenu(@RequestParam long menuId){
        try {
            roleService.delMenu(menuId);
        }catch (Exception e){
            e.printStackTrace();
            return RestUtil.Error(500);
        }
        return RestUtil.Success();
    }

    /**
     * 登录日志列表页
     */
    @RequestMapping("/loginLogs")
    public String loginLogList(@PageableDefault(sort = { "loginTime" }, direction = Sort.Direction.DESC)
                                       Pageable pageable, @Valid LoginLog loginLog, Model model){
        Page<LoginLog> logs = logService.getLoginLogList(pageable,loginLog);
        model.addAttribute("logList",logs);
        model.addAttribute("log",loginLog);
        return "system/logList";
    }

    /**
     * 文件上传
     * @param file
     * @return
     */
    @RequestMapping("/upload")
    @ResponseBody
    public ModelMap uploadAvatar(@RequestParam("file")MultipartFile file){
        if(!file.isEmpty()){
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(file.getOriginalFilename())));
                out.write(file.getBytes());
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return RestUtil.Error(222);
            } catch (IOException e) {
                e.printStackTrace();
                return RestUtil.Error(222);
            }
            return RestUtil.Success("/"+file.getOriginalFilename());
        }else{
            return RestUtil.Error(222,"文件为空");
        }
    }
}
