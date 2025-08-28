#!/bin/bash

# RUI Cloud Docker 环境管理脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_message() {
    echo -e "${2}${1}${NC}"
}

# 打印标题
print_title() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}    RUI Cloud Docker 环境管理脚本${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo
}

# 检查Docker和Docker Compose
check_requirements() {
    if ! command -v docker &> /dev/null; then
        print_message "错误: Docker 未安装或未在PATH中" "$RED"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_message "错误: Docker Compose 未安装或未在PATH中" "$RED"
        exit 1
    fi
}

# 显示菜单
show_menu() {
    echo "请选择操作："
    echo "1. 启动基础服务 (MySQL + Redis)"
    echo "2. 启动完整服务 (包含监控组件)"
    echo "3. 启动代理服务 (包含Nginx)"
    echo "4. 停止所有服务"
    echo "5. 重启服务"
    echo "6. 查看服务状态"
    echo "7. 查看服务日志"
    echo "8. 清理数据卷"
    echo "9. 退出"
    echo
}

# 启动基础服务
start_basic() {
    print_message "启动基础服务 (MySQL + Redis)..." "$YELLOW"
    docker-compose up -d mysql redis
    print_message "基础服务启动完成！" "$GREEN"
    echo "MySQL: localhost:3306 (用户名: root, 密码: 123456)"
    echo "Redis: localhost:6379"
    echo
}

# 启动完整服务
start_monitoring() {
    print_message "启动完整服务 (包含监控组件)..." "$YELLOW"
    docker-compose --profile monitoring up -d
    print_message "完整服务启动完成！" "$GREEN"
    echo "MySQL: localhost:3306"
    echo "Redis: localhost:6379"
    echo "Jaeger UI: http://localhost:16686"
    echo "Zipkin UI: http://localhost:9411"
    echo "Prometheus: http://localhost:9090"
    echo "Grafana: http://localhost:3000 (用户名: admin, 密码: admin123)"
    echo
}

# 启动代理服务
start_proxy() {
    print_message "启动代理服务 (包含Nginx)..." "$YELLOW"
    docker-compose --profile proxy up -d
    print_message "代理服务启动完成！" "$GREEN"
    echo "应用访问地址: http://localhost"
    echo "Nginx配置: docker/nginx/"
    echo
}

# 停止所有服务
stop_all() {
    print_message "停止所有服务..." "$YELLOW"
    docker-compose --profile monitoring --profile proxy down
    print_message "所有服务已停止！" "$GREEN"
    echo
}

# 重启服务
restart_services() {
    print_message "重启服务..." "$YELLOW"
    docker-compose --profile monitoring --profile proxy restart
    print_message "服务重启完成！" "$GREEN"
    echo
}

# 查看服务状态
show_status() {
    print_message "查看服务状态..." "$YELLOW"
    docker-compose ps
    echo
}

# 查看服务日志
show_logs() {
    echo -n "请输入服务名称 (mysql/redis/jaeger/zipkin/prometheus/grafana/nginx，留空查看所有): "
    read service_name
    
    if [ -z "$service_name" ]; then
        docker-compose logs --tail=50 -f
    else
        docker-compose logs --tail=50 -f "$service_name"
    fi
}

# 清理数据卷
cleanup_volumes() {
    print_message "警告：此操作将删除所有数据卷，数据将无法恢复！" "$RED"
    echo -n "确认删除所有数据？(y/N): "
    read confirm
    
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        print_message "停止所有服务..." "$YELLOW"
        docker-compose --profile monitoring --profile proxy down
        
        print_message "删除数据卷..." "$YELLOW"
        docker volume rm rui-cloud_mysql_data rui-cloud_redis_data rui-cloud_prometheus_data rui-cloud_grafana_data 2>/dev/null || true
        
        print_message "数据清理完成！" "$GREEN"
    else
        print_message "操作已取消" "$YELLOW"
    fi
    echo
}

# 主函数
main() {
    print_title
    check_requirements
    
    while true; do
        show_menu
        echo -n "请输入选项 (1-9): "
        read choice
        
        case $choice in
            1) start_basic ;;
            2) start_monitoring ;;
            3) start_proxy ;;
            4) stop_all ;;
            5) restart_services ;;
            6) show_status ;;
            7) show_logs ;;
            8) cleanup_volumes ;;
            9) 
                print_message "感谢使用 RUI Cloud Docker 环境管理脚本！" "$GREEN"
                exit 0
                ;;
            *) 
                print_message "无效选项，请重新选择" "$RED"
                echo
                ;;
        esac
    done
}

# 运行主函数
main "$@"