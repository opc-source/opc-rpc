# Opc-Rpc
Good practice of gRPC

- 核心逻辑借鉴 [Nacos](https://github.com/alibaba/nacos)，client 与 server 全双工通讯，使得 server 可主动请求 client，感谢如此优秀的项目。
- 更多做 gRPC 的最佳实践

## 模块说明
- rpc-api      抽象模型模块
- rpc-core     通用能力模块
- rpc-client   客户端仅需它
- rpc-server   服务端仅需它

```
    +---------+          +------------+          +------------+
    | rpc-api |  <-----  |  rpc-core  |  <-----  | rpc-client |
    +---------+          +------------+          +------------+
                               个
                               ｜
                               ｜                +---—--------+ 
                               ｜--------------  | rpc-server | 
                                                 +------------+ 
```

# License
[Apache License Version 2.0](LICENSE)
