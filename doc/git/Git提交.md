**暂存现场 -> 同步 Master -> 把 Slave 分支“嫁接”到最新的 Master 上 -> 恢复现场**。

### 🛠️ 操作步骤

#### 第一步：保护现场（暂存未提交代码）
先把还没提交的代码藏起来，防止一会儿切换分支时弄丢或冲突。

```bash
# 1. 确保你在 slave 分支
git checkout slave

# 2. 暂存未提交的修改和新文件
git stash push -u -m "B机器未提交的临时修改"
```

#### 第二步：更新 Master 分支
让 B 机器的 `master` 追上远程（也就是 A 机器合并后的状态）。

```bash
# 1. 切换到 master
git checkout master

# 2. 拉取远程最新信息
git fetch origin

# 3. 强制让本地 master 和远程完全一致
git reset --hard origin/master
```
*此时，你的本地 `master` 已经包含了 A 的那第 17 次提交以及合并记录。*

#### 第三步：整理 Slave 分支（核心步骤：变基）
这是最关键的一步。我们要把 B 的 `slave` 分支的“地基”从旧版本换成最新的 `master`。

由于前 16 次提交是一样的，Git 会自动识别并跳过它们，只处理差异部分（如果有的话），或者仅仅是把指针指过去。

```bash
# 1. 切回 slave 分支
git checkout slave

# 2. 执行变基
# 意思是以 origin/master 为新的地基，重新播放 slave 的提交
git rebase origin/master
```

> **预期结果**：
> Git 会发现你的前 16 次提交已经在 `origin/master` 里了，所以它会显示类似 `Successfully rebased and updated...` 的信息，或者提示 `Current branch slave is up to date.`。
>
> *注意：如果你的第 16 次提交和 A 的第 17 次提交修改了同一个文件的同一行代码，这里会报冲突。如果没有重叠修改，就会瞬间完成。*

#### 第四步：恢复现场
现在你的 `slave` 分支已经是基于最新的 `master` 了，可以把刚才藏起来的代码拿回来了。

```bash
# 取出暂存的代码
git stash pop
```

#### 第五步：提交并推送
现在你的工作区是最新的，包含你未提交的修改。

```bash
# 1. 提交你的修改
git add .
git commit -m "feat: 完成 B 机器的新功能"

# 2. 推送到远程
# 因为你做了 rebase，历史轨迹变了，通常需要强制推送
git push --force-with-lease origin slave
```



## 标准的“功能分支合并到主分支”场景

### 🛠️ 操作步骤

#### 第一步：确保 Master 是最新的
在合并之前，先拉取远程最新的 `master` 代码，避免冲突。

```bash
# 1. 切换到 master 分支
git checkout master

# 2. 拉取远程最新代码
git pull origin master
```

#### 第二步：合并 Slave 分支到 Master
将 `slave` 分支的修改合并到当前的 `master` 分支。

```bash
# 在 master 分支上执行合并
git merge slave
```

**可能出现的两种情况：**
*   **无冲突**：Git 会自动生成一个合并提交，或者直接快进（Fast-forward）。
*   **有冲突**：Git 会提示冲突文件。你需要手动打开文件解决冲突，然后执行：
    ```bash
    git add .
    git commit -m "解决合并冲突"
    ```

#### 第三步：推送 Master 到远程
合并完成后，将本地的 `master` 推送到远程仓库。

```bash
git push origin master
```

#### 第四步：（可选）清理本地 Slave 分支
如果你不再需要本地的 `slave` 分支，可以删除它保持整洁。

```bash
# 删除本地 slave 分支
git branch -d slave

