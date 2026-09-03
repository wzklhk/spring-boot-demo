package com.example.demo.auth;

import com.example.demo.pojo.entity.Role;
import com.example.demo.pojo.entity.User;
import com.example.demo.pojo.entity.UserRole;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 从数据库加载用户用于登录认证（适配 Spring Security）
 * authorities 按 RBAC 动态授予：user_role → role.code → ROLE_<CODE>
 * （无角色用户仅可查看；ADMIN 角色拥有全部查看/修改权限）
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        // 角色编码 → authorities（如 ADMIN → ROLE_ADMIN）；无角色时为空列表
        List<String> authorities = userRoleRepository.findByUserId(user.getId()).stream()
                .map(UserRole::getRoleId)
                .flatMap(roleId -> roleRepository.findById(roleId).stream())
                .map(role -> "ROLE_" + role.getCode())
                .toList();
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities.toArray(new String[0]))
                .build();
    }
}
