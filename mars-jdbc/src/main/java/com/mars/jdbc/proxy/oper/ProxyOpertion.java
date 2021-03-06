package com.mars.jdbc.proxy.oper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mars.jdbc.annotation.MarsGet;
import com.mars.jdbc.annotation.MarsSelect;
import com.mars.jdbc.annotation.MarsUpdate;
import com.mars.jdbc.helper.model.PageModel;
import com.mars.jdbc.helper.model.PageParamModel;
import com.mars.jdbc.helper.templete.JdbcTemplate;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 代理操作数据库
 */
public class ProxyOpertion {

    /**
     * 根据主键查询一条数据
     * @param marsGet 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @param method 方法
     * @return 数据
     * @throws Exception 异常
     */
    public static Object get(MarsGet marsGet, String dataSourceName, Object param, Method method) throws Exception {

        StringBuffer sql = new StringBuffer();
        sql.append("select * from ");
        sql.append(marsGet.tableName());
        sql.append(" where ");
        sql.append(marsGet.primaryKey());
        sql.append(" = ?");

        return JdbcTemplate.get(dataSourceName).selectOne(sql.toString(),new Object[]{param},method.getReturnType());
    }

    /**
     * 根据sql查询数据
     * @param marsSelect 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @return 数据
     * @throws Exception 异常
     */
    public static Object select(MarsSelect marsSelect, String dataSourceName, Object param, Method method) throws Exception {
        if(marsSelect.page()){
            if(!method.getReturnType().equals(PageModel.class)){
                throw new Exception("方法["+method.getName()+"]由于设置了分页，所以它的返回类型必须是["+PageModel.class.getName()+"]类型");
            }
            if(!(param instanceof PageParamModel)){
                throw new Exception("方法["+method.getName()+"]由于设置了分页，所以它的参数必须是["+PageParamModel.class.getName()+"]类型");
            }
            return JdbcTemplate.get(dataSourceName).selectPageList(marsSelect.sql(),(PageParamModel)param,marsSelect.resultType());
        } else {
            if(method.getReturnType().equals(List.class)){
                return JdbcTemplate.get(dataSourceName).selectList(marsSelect.sql(),param,marsSelect.resultType());
            } else {
                return JdbcTemplate.get(dataSourceName).selectOne(marsSelect.sql(),param,marsSelect.resultType());
            }
        }
    }

    /**
     * 单表增删改
     * @param marsUpdate 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @return 数据
     * @throws Exception 异常
     */
    public static Object update(MarsUpdate marsUpdate, String dataSourceName, Object param) throws Exception {
        switch (marsUpdate.operType()){
            case DELETE:
                return doDelete(marsUpdate,dataSourceName,param);
            case UPDATE:
                return doUpdate(marsUpdate,dataSourceName,param);
            case INSERT:
                return doInsert(marsUpdate,dataSourceName,param);
            default:
                throw new Exception("没有指定operType");
        }
    }

    /**
     * 单表增删改
     * @param marsUpdate 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @return 数据
     * @throws Exception 异常
     */
    private static Object doDelete(MarsUpdate marsUpdate, String dataSourceName, Object param) throws Exception {

        StringBuffer sql = new StringBuffer();
        sql.append("delete from ");
        sql.append(marsUpdate.tableName());
        sql.append(" where ");
        sql.append(marsUpdate.primaryKey());
        sql.append(" = ?");

        return JdbcTemplate.get(dataSourceName).update(sql.toString(),new Object[]{param});
    }

    /**
     * 单表增删改
     * @param marsUpdate 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @return 数据
     * @throws Exception 异常
     */
    private static Object doInsert(MarsUpdate marsUpdate, String dataSourceName, Object param) throws Exception {
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(param));

        StringBuffer sql = new StringBuffer();
        sql.append("insert into ");
        sql.append(marsUpdate.tableName());
        sql.append("(");
        boolean isFirst = true;
        for(String key : jsonObject.keySet()){
            Object val = jsonObject.get(key);
            if(val != null){
                if(!isFirst){
                    sql.append(",");
                }
                sql.append(key);
                isFirst = false;
            }
        }
        sql.append(") values(");
        isFirst = true;
        for(String key : jsonObject.keySet()){
            Object val = jsonObject.get(key);
            if(val != null){
                if (!isFirst) {
                    sql.append(",");
                }
                sql.append("#{");
                sql.append(key);
                sql.append("}");
                isFirst = false;
            }
        }
        sql.append(")");

        return JdbcTemplate.get(dataSourceName).update(sql.toString(),param);
    }

    /**
     * 单表增删改
     * @param marsUpdate 注解
     * @param dataSourceName 数据源
     * @param param 参数
     * @return 数据
     * @throws Exception 异常
     */
    private static Object doUpdate(MarsUpdate marsUpdate, String dataSourceName, Object param) throws Exception {
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(param));
        StringBuffer sql = new StringBuffer();
        sql.append("update ");
        sql.append(marsUpdate.tableName());
        sql.append(" set ");
        boolean isFirst = true;
        for(String key : jsonObject.keySet()){
            Object val = jsonObject.get(key);
            if(val != null && !key.equals(marsUpdate.primaryKey())) {
                if (!isFirst) {
                    sql.append(",");
                }
                sql.append(key);
                sql.append(" = #{");
                sql.append(key);
                sql.append("}");
                isFirst = false;
            }
        }
        sql.append(builderWhere(marsUpdate));

        return JdbcTemplate.get(dataSourceName).update(sql.toString(),param);
    }

    /**
     * 构建条件
     * @param marsUpdate 注解
     * @return 条件
     */
    private static StringBuffer builderWhere(MarsUpdate marsUpdate){
        StringBuffer sql = new StringBuffer();
        sql.append(" where ");
        sql.append(marsUpdate.primaryKey());
        sql.append(" = #{");
        sql.append(marsUpdate.primaryKey());
        sql.append("}");
        return sql;
    }
}
