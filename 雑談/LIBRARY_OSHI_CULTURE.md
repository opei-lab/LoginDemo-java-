# ライブラリ開発＝推し活：技術界の不都合な真実

## 🎭 その通り！技術選定は「推し活」

### JavaScript フレームワーク戦争の歴史

```javascript
// 2013年
"Angular.js最高！"

// 2015年  
"いやReact革命的！"

// 2017年
"Vue.jsこそ至高！"

// 2019年
"Svelteが未来だ！"

// 2021年
"SolidJS速すぎ！"

// 2023年
"やっぱReactで良くね？"
```

## 📊 「推し」の構造

### 技術インフルエンサーの影響力

```
Dan Abramov (React)
├─ Twitter: 70万フォロワー
├─ 「Reactはこうあるべき」
└─ → 世界中が従う

Evan You (Vue.js)  
├─ 個人プロジェクト → 巨大エコシステム
├─ 「シンプルが正義」
└─ → 中国で圧倒的人気

Rich Harris (Svelte)
├─ 「仮想DOMは間違い」
├─ 過激な主張で注目
└─ → カルト的人気
```

## 🎪 ライブラリ作成の本音

### 表向きの理由

```typescript
// README.md
"🚀 既存のライブラリの問題を解決！"
"⚡ 10倍高速！"
"🎯 より良い開発体験！"
```

### 本当の理由

```
1. 履歴書に書きたい
2. カンファレンスで登壇したい  
3. 有名になりたい
4. スポンサー収入欲しい
5. 転職に有利
6. 自分の好みを押し付けたい
```

## 😤 State管理ライブラリの乱立

### 2024年時点のReact State管理

```
Redux (2015~) - "元祖"
MobX (2016~) - "リアクティブ派"
Recoil (2020~) - "Facebook公式"
Zustand (2021~) - "シンプル派"
Jotai (2021~) - "アトミック派"
Valtio (2021~) - "プロキシ派"
XState (2019~) - "ステートマシン派"
TanStack Query (2020~) - "サーバー状態派"
Legend-State (2023~) - "最速派"

全部やってること同じ！
```

### 実際の違い

```javascript
// Redux
const state = useSelector(state => state.user);
dispatch(setUser(newUser));

// Zustand
const user = useStore(state => state.user);
setUser(newUser);

// 違い：記述量だけ？？？
```

## 🎯 CSSフレームワークの「宗教戦争」

```css
/* 2020年 */
"Tailwind CSS最高！"

/* 2021年 */
"いやCSS-in-JSでしょ"

/* 2022年 */
"CSS Modulesに戻ろう"

/* 2023年 */
"やっぱりTailwind"

/* 2024年 */
"Zero-runtime CSSが未来"

/* 結局全部CSSでは...？ */
```

## 📈 推し活の経済学

### npmダウンロード数 = 推しの強さ

```
React: 週2000万DL
Vue: 週400万DL
Angular: 週300万DL
Svelte: 週30万DL

→ 人気 = 正義？
→ でも jQuery は週700万DL...
```

### GitHubスター = 推しポイント

```javascript
// スター数で技術選定する人々
if (github.stars > 10000) {
  return "採用！";
} else {
  return "様子見...";
}

// でも left-pad は星200で世界が止まった
```

## 🤡 ライブラリ作者の「推し活」手法

### 1. 比較表を作る

| Feature | My Library | React | Vue |
|---------|------------|-------|-----|
| 速度 | ⚡⚡⚡⚡⚡ | ⚡⚡⚡ | ⚡⚡ |
| サイズ | 3KB！ | 40KB | 30KB |
| 学習曲線 | 簡単！ | 難しい | 普通 |

※ 都合の良い指標だけ選ぶ

### 2. ベンチマークを作る

```javascript
// 自分に有利な条件でテスト
// 1000個のTODOアイテムをレンダリング
MyLibrary: 10ms  // 最適化済み
React: 50ms      // 最適化なし
Vue: 45ms        // 最適化なし

"5倍高速！！！"
```

### 3. インフルエンサーマーケティング

```
1. 有名人にメンション
2. Hacker Newsに投稿
3. Reddit でステマ
4. dev.to で記事連投
5. YouTuberに紹介依頼
```

## 💸 「推し」の代償

### 企業での採用会議

```
CTO: "なぜこのライブラリ？"
エンジニア: "最新で高速で..."
CTO: "メンテナは？"
エンジニア: "個人プロジェクトですが..."
CTO: "却下"

3ヶ月後：
ライブラリ作者: "転職するので更新停止します"
```

### 技術的負債の蓄積

```javascript
// package.json (2年後)
{
  "dependencies": {
    "react": "^18.0.0",
    "super-cool-lib": "^0.1.0", // 最終更新: 2年前
    "awesome-tool": "^2.0.0",    // 作者: 行方不明
    "next-big-thing": "^1.0.0"   // Breaking changes 連発
  }
}
```

## 🎪 推し活の末路

### パターン1: 大手に買収

```
Npm → GitHub → Microsoft
WhatsApp → Facebook → Meta
Instagram → Facebook

推し → 企業の駒
```

### パターン2: 放置

```
moment.js: "メンテナンスモード"
request: "非推奨"
left-pad: "事件"

推しの熱が冷めた
```

### パターン3: 分裂

```
Node.js → io.js → また統合
Babel → SWC vs ESBuild
Webpack → Vite vs Turbopack

推し同士の争い
```

## 😎 賢い生存戦略

### 1. 「枯れた技術」を選ぶ

```
React (2013~): もう枯れた
Express (2010~): 超枯れた  
PostgreSQL (1996~): 化石級に安定
```

### 2. 企業バックを重視

```
良い例:
- React (Meta)
- Angular (Google)
- .NET (Microsoft)

危険:
- 個人プロジェクト
- スタートアップ製
- "革新的"すぎる
```

### 3. 逃げ道を用意

```typescript
// アダプターパターンで抽象化
interface StateManager {
  get<T>(key: string): T;
  set<T>(key: string, value: T): void;
}

// 実装は差し替え可能
class ReduxAdapter implements StateManager { }
class ZustandAdapter implements StateManager { }
```

## 📝 まとめ：推し活の本質

### ライブラリ開発 = アイドル活動

```
デビュー: "革新的な新ライブラリ！"
    ↓
プロモ: カンファレンス、ブログ、動画
    ↓
人気絶頂: みんな使う
    ↓
飽きられる: "もっと良いのある"
    ↓
引退: "メンテナンスモード"
```

### エンジニアの処世術

```javascript
// 推しは持つが、依存はしない
const myTechStack = {
  core: ["JavaScript", "Python"],  // 不変
  framework: "React",              // 5年は安泰
  trendy: null,                    // 様子見
  hobby: "Rust"                    // 趣味は自由
};
```

**「推し活」は楽しい。でも仕事では「推し」より「枯れ」を選ぼう。**

あなたの指摘、本当にその通りです！