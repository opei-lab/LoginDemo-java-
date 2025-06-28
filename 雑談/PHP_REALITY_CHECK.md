# PHP オワコン説の真相：Web の79%を支える言語

## 😅 PHPのイメージ問題

### よく言われること

```php
// 「PHPはクソ」と言われる理由
$array = array();  // 昔の書き方
mysql_query($sql); // 非推奨関数
$_GET['id'];       // セキュリティホール
echo "<html>";     // HTMLと混在

// 型がゆるい
"123" + 456 == 579  // 文字列が勝手に数値に
```

### 悪評の原因

```
1. 初心者が書いた危険なコード
2. WordPress のスパゲッティ
3. 古いPHP5時代のイメージ
4. 「なんでも書ける」ゆるさ
5. モダンな印象がない
```

## 📊 でも現実は...

### Web サイトのシェア（2024年）

```
全Webサイトの79%がPHPを使用

内訳:
- WordPress: 43%
- その他CMS: 15%
- カスタム開発: 21%

JavaScript (Node.js): 3%
Python: 2%
Ruby: 1%
```

### 有名サービスの使用状況

```php
// まだPHPで動いている
Facebook (一部)
Wikipedia
WordPress.com
Slack (一部)
Etsy
Mailchimp

// PHPからの移行組
Twitter → Scala/Java
Yahoo → Node.js
```

## 🚀 現代のPHP（PHP 8.3）

### もはや別言語レベルの進化

```php
<?php
// 昔のPHP（PHP 5）
class User {
    private $name;
    private $email;
    
    public function __construct($name, $email) {
        $this->name = $name;
        $this->email = $email;
    }
}

// 現代のPHP（PHP 8）
class User {
    public function __construct(
        private string $name,
        private string $email,
    ) {}
}

// 型宣言
function calculate(int $a, int $b): int {
    return $a + $b;
}

// Null安全演算子
$country = $user?->getAddress()?->getCountry() ?? 'Unknown';

// Match式（Switch改良版）
$result = match($status) {
    'pending' => 'waiting',
    'done' => 'completed',
    default => 'unknown'
};

// アトリビュート（アノテーション）
#[Route('/api/users', methods: ['GET'])]
public function getUsers(): array {
    return User::all();
}
```

### パフォーマンスの改善

```
PHP 5.6: 100 req/s
PHP 7.0: 200 req/s
PHP 8.0: 300 req/s + JIT
PHP 8.3: 350 req/s

3.5倍高速化！
```

## 💪 PHPの強み

### 1. 圧倒的なホスティング対応

```
対応レンタルサーバー:
PHP: 99.9%
Node.js: 20%
Python: 15%
Ruby: 10%

月500円でも動く
```

### 2. 最速の開発速度

```php
// 1ファイルでWebアプリ
<?php
// index.php
$users = ['Alice', 'Bob'];
?>
<h1>Users</h1>
<ul>
    <?php foreach($users as $user): ?>
        <li><?= htmlspecialchars($user) ?></li>
    <?php endforeach; ?>
</ul>

// これだけで動く！
```

### 3. 現代的なフレームワーク

```php
// Laravel（PHPのRails）
Route::get('/users', [UserController::class, 'index']);

class UserController extends Controller {
    public function index() {
        return User::paginate(15);
    }
}

// Symfony（エンタープライズ向け）
#[Route('/api/users', name: 'api_users')]
public function users(): JsonResponse {
    return $this->json($this->userRepository->findAll());
}
```

## 🎯 PHPが得意な分野

### 向いている

```
✅ 中小規模のWebサービス
✅ CMS・ブログ
✅ ECサイト
✅ 企業サイト
✅ API開発
✅ 短期開発案件
```

### 向いていない

```
❌ リアルタイム処理（WebSocket）
❌ 機械学習
❌ システムプログラミング
❌ モバイルアプリ
❌ 大規模な非同期処理
```

## 💰 PHPエンジニアの現実

### 求人市場（2024年）

```
求人数:
東京: 8,000件
大阪: 2,000件
福岡: 1,000件

年収:
初級: 300-400万
中級: 400-600万
上級: 600-800万
Laravel専門: 700-900万
```

### 案件の種類

```
40%: WordPress カスタマイズ
30%: Laravel/CakePHP 開発
20%: レガシーシステム保守
10%: モダンPHP新規開発
```

## 🔮 PHPの未来

### 生き残る理由

```
1. WordPress が死なない限り不滅
2. 安いサーバーで動く
3. 学習コストが低い
4. 大量の既存資産
5. 継続的な改善
```

### 衰退の兆候

```
1. 新規スタートアップは選ばない
2. モダンなイメージがない
3. エンジニアの高齢化
4. Node.js/Pythonへの移行増
```

## 📝 PHPを選ぶべき人

### おすすめケース

```php
if ($あなた === "Web制作会社勤務") {
    return "PHP必須";
} elseif ($あなた === "副業したい") {
    return "WordPress案件多数";
} elseif ($あなた === "地方在住") {
    return "PHP案件が多い";
} else {
    return "他の選択肢も検討";
}
```

### Laravel を学ぶ価値

```
Laravel = PHPの最高峰
- モダンな書き方
- 豊富な機能
- 活発なコミュニティ
- 高単価案件

PHPやるならLaravel一択
```

## 🏁 結論：場所による

### 日本では

```
オワコン度: ★★★☆☆
理由:
- WordPress需要は不滅
- 地方では現役
- 保守案件多数
- でも新規は減少
```

### 世界では

```
オワコン度: ★★☆☆☆
理由:
- Laravelが人気
- 開発途上国で主流
- コスト重視なら最適
```

## 💡 最終判定

**PHPは「オワコン」ではなく「成熟」**

```
良い点:
- 仕事はまだある
- 習得が簡単
- すぐに稼げる

悪い点:
- キャリアの上限
- モダンじゃない
- 将来性は微妙
```

### 現実的なアドバイス

```php
// 2024年の選択
if (目的 === "すぐに稼ぎたい") {
    学ぶ = "WordPress + PHP基礎";
} elseif (目的 === "モダンなWeb開発") {
    学ぶ = "Laravel";
} elseif (目的 === "将来性重視") {
    別の言語を検討();
}
```

**「PHP + Laravel」なら、まだ5-10年は戦える。**
**でも新人なら TypeScript から始める方が賢明。**