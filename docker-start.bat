@echo off
chcp 65001 >nul
echo ========================================
echo    RUI Cloud Docker 环境管理脚本
echo ========================================
echo.

set "COMPOSE_FILE=docker-compose.yml"

:menu
echo 请选择操作：
echo 1. 启动基础服务 (MySQL + Redis)
echo 2. 启动完整服务 (包含监控组件)
echo 3. 启动代理服务 (包含Nginx)
echo 4. 停止所有服务
echo 5. 重启服务
echo 6. 查看服务状态
echo 7. 查看服务日志
echo 8. 清理数据卷
echo 9. 退出
echo.
set /p choice=请输入选项 (1-9): 

if "%choice%"=="1" goto start_basic
if "%choice%"=="2" goto start_monitoring
if "%choice%"=="3" goto start_proxy
if "%choice%"=="4" goto stop_all
if "%choice%"=="5" goto restart
if "%choice%"=="6" goto status
if "%choice%"=="7" goto logs
if "%choice%"=="8" goto cleanup
if "%choice%"=="9" goto exit

echo 无效选项，请重新选择
goto menu

:start_basic
echo 启动基础服务 (MySQL + Redis)...
docker-compose up -d mysql redis
echo 基础服务启动完成！
echo MySQL: localhost:3306 (用户名: root, 密码: 123456)
echo Redis: localhost:6379
goto menu

:start_monitoring
echo 启动完整服务 (包含监控组件)...
docker-compose --profile monitoring up -d
echo 完整服务启动完成！
echo MySQL: localhost:3306
echo Redis: localhost:6379
echo Jaeger UI: http://localhost:16686
echo Zipkin UI: http://localhost:9411
echo Prometheus: http://localhost:9090
echo Grafana: http://localhost:3000 (用户名: admin, 密码: admin123)
goto menu

:start_proxy
echo 启动代理服务 (包含Nginx)...
docker-compose --profile proxy up -d
echo 代理服务启动完成！
echo 应用访问地址: http://localhost
echo Nginx配置: docker/nginx/
goto menu

:stop_all
echo 停止所有服务...
docker-compose --profile monitoring --profile proxy down
echo 所有服务已停止！
goto menu

:restart
echo 重启服务...
docker-compose --profile monitoring --profile proxy restart
echo 服务重启完成！
goto menu

:status
echo 查看服务状态...
docker-compose ps
goto menu

:logs
echo 查看服务日志...
set /p service=请输入服务名称 (mysql/redis/jaeger/zipkin/prometheus/grafana/nginx): 
if "%service%"=="" (
    docker-compose logs --tail=50 -f
) else (
    docker-compose logs --tail=50 -f %service%
)
goto menu

:cleanup
echo 警告：此操作将删除所有数据卷，数据将无法恢复！
set /p confirm=确认删除所有数据？(y/N): 
if /i "%confirm%"=="y" (
    echo 停止所有服务...
    docker-compose --profile monitoring --profile proxy down
    echo 删除数据卷...
    docker volume rm rui-cloud_mysql_data rui-cloud_redis_data rui-cloud_prometheus_data rui-cloud_grafana_data 2>nul
    echo 数据清理完成！
) else (
    echo 操作已取消
)
goto menu

:exit
echo 感谢使用 RUI Cloud Docker 环境管理脚本！
pause
exit /b 0