//! SQL 查询辅助工具
//!
//! 提供跨 feature 共享的 SQL 片段,例如 ILIKE 通配符转义。

/// 对 ILIKE 模式中的通配符(`%` / `_`)和转义符(`\`)进行转义。
///
/// 配合 `ILIKE '%' || $N || '%' ESCAPE '\'` 使用,
/// 防止用户输入 `%` 导致全表扫描、或输入 `\` 破坏转义链。
pub fn escape_ilike(input: &str) -> String {
    let mut out = String::with_capacity(input.len());
    for c in input.chars() {
        match c {
            '\\' | '%' | '_' => {
                out.push('\\');
                out.push(c);
            }
            _ => out.push(c),
        }
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn escapes_wildcards_and_backslash() {
        assert_eq!(escape_ilike("hello"), "hello");
        assert_eq!(escape_ilike("50%off"), r"50\%off");
        assert_eq!(escape_ilike("a_b"), r"a\_b");
        assert_eq!(escape_ilike(r"c\d"), r"c\\d");
        assert_eq!(escape_ilike("%_\\"), r"\%\_\\");
    }
}
