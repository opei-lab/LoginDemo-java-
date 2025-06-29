# Spring Boot アプリケーションのフロー図

## 1. MVC アーキテクチャのフロー

```mermaid
graph TD
    A[ブラウザからのリクエスト] --> B[DispatcherServlet]
    B --> C[Security Filter Chain]
    C --> D[Controller]
    D --> E[Service]
    E --> F[Repository]
    F --> G[データベース]
    F --> H[セッション管理]
    
    subgraph "コントローラー層"
        D
    end
    
    subgraph "ビジネス層"
        E
    end
    
    subgraph "データ層"
        F
        G
    end
    
    subgraph "セキュリティ層"
        H
    end
    
    G --> F
    H --> F
    F --> E
    E --> D
    D --> I[View]
    I --> J[Thymeleafテンプレート]
    J --> K[レスポンス]
    K --> A
```

## 2. セキュリティフロー

```mermaid
graph TD
    A[リクエスト] --> B[Security Filter Chain]
    B --> C{認証チェック}
    C -->|認証済み| D[Controller]
    C -->|未認証| E[ログイン画面]
    E --> F[認証処理]
    F --> G[セッション作成]
    G --> H[認証成功]
    H --> D
    
    subgraph "セキュリティチェック"
        C
    end
    
    subgraph "認証処理"
        E --> F --> G --> H
    end
```

## 3. データフローとDI

```mermaid
classDiagram
    class Request {
        +HttpServletRequest
        +HttpServletResponse
    }
    
    class DispatcherServlet {
        +doDispatch()
    }
    
    class Controller {
        @Autowired
        +handleRequest()
    }
    
    class Service {
        @Autowired
        +businessLogic()
    }
    
    class Repository {
        @Autowired
        +dataAccess()
    }
    
    class Session {
        +setAttribute()
        +getAttribute()
    }
    
    Request --> DispatcherServlet
    DispatcherServlet --> Controller
    Controller --> Service
    Service --> Repository
    Controller --> Session
    
    classDef spring fill:#3498db,stroke:#333,stroke-width:2px
    classDef controller fill:#e74c3c,stroke:#333,stroke-width:2px
    classDef service fill:#2ecc71,stroke:#333,stroke-width:2px
    classDef repository fill:#9b59b6,stroke:#333,stroke-width:2px
    
    class DispatcherServlet, Controller, Service, Repository
```

## アノテーションの関連性

```mermaid
classDiagram
    class Controller {
        @Controller
        @RequestMapping
        +handleRequest()
    }
    
    class Service {
        @Service
        @Transactional
        +businessLogic()
    }
    
    class Repository {
        @Repository
        @Transactional
        +dataAccess()
    }
    
    class Security {
        @EnableWebSecurity
        @Configuration
        +configure()
    }
    
    Controller --> Service
    Service --> Repository
    Security --> Controller
    
    classDef annotation fill:#f1c40f,stroke:#333,stroke-width:2px
    class Controller, Service, Repository, Security
```

## セッション管理フロー

```mermaid
graph TD
    A[リクエスト] --> B[セッションチェック]
    B -->|新規| C[セッション作成]
    B -->|既存| D[セッション取得]
    C --> E[セッションID生成]
    E --> F[セッション属性保存]
    F --> G[セッションIDクッキーに保存]
    D --> F
    F --> H[セッション属性取得]
    H --> I[セッション終了]
    I --> J[セッション削除]
```

## ビューとテンプレートエンジン

```mermaid
graph TD
    A[Controller] --> B[ViewResolver]
    B --> C[Thymeleafテンプレート]
    C --> D[テンプレート処理]
    D --> E[HTML生成]
    E --> F[レスポンス]
    
    subgraph "テンプレートエンジン"
        C --> D --> E
    end
```

## インジェクションのフロー

```mermaid
graph TD
    A[Bean定義] --> B[ApplicationContext]
    B --> C[Bean生成]
    C --> D[依存関係解決]
    D --> E[DI実行]
    
    subgraph "DIコンテナ"
        B --> C --> D --> E
    end
```
