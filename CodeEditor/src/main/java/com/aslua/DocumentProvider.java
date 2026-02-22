package com.aslua;


import java.util.*;

import com.aslua.language.Language;

/**
 * DocumentProvider类用于访问底层文本缓冲区的字符。
 * 使用步骤如下：
 * 1. 调用 seekChar(offset) 方法设置开始迭代的位置
 * 2. 调用 hasNext() 判断是否有下一个字符
 * 3. 调用 next() 获取下一个字符
 * 如果有多个 DocumentProvider 指向相同的 Document，
 * 一个 DocumentProvider 的更改不会通知其他 DocumentProvider。
 * 如有需要可以实现发布/订阅接口。
 */
public class DocumentProvider implements CharSequence {

	@Override
	public int length() {
		return _theText.length();
	}

	/** 当前文本位置，范围 [0, _theText.getTextLength()) */
	private int _currIndex;
	private final Document _theText;

	// 构造方法，根据文本度量信息初始化 DocumentProvider
	public DocumentProvider(Document.TextFieldMetrics metrics) {
		_currIndex = 0;
		_theText = new Document(metrics);
	}

	// 构造方法，通过 Document 对象初始化 DocumentProvider
	public DocumentProvider(Document doc) {
		_currIndex = 0;
		_theText = doc;
	}

	// 拷贝构造方法
	public DocumentProvider(DocumentProvider rhs) {
		_currIndex = 0;
		_theText = rhs._theText;
	}

	/**
	 * 获取从 charOffset 开始、长度不超过 maxChars 的子字符串
	 */
	public CharSequence subSequence(int charOffset, int maxChars) {
		return _theText.subSequence(charOffset, maxChars);
	}

	// 返回指定字符位置的字符，如果无效则返回 NULL_CHAR
	public char charAt(int charOffset) {
		if (_theText.isValid(charOffset)) {
			return _theText.charAt(charOffset);
		} else {
			return Language.NULL_CHAR;
		}
	}

	// 获取指定行的文本内容
	public String getRow(int rowNumber) {
		return _theText.getRow(rowNumber);
	}

	/**
	 * 获取指定字符位置所在的行号
	 */
	public int findRowNumber(int charOffset) {
		return _theText.findRowNumber(charOffset);
	}

	/**
	 * 获取指定字符位置所在的行号。行与列不同，行可以被自动换行成多个列。
	 */
	public int findLineNumber(int charOffset) {
		return _theText.findLineNumber(charOffset);
	}

	/**
	 * 获取指定行号的第一个字符偏移量
	 */
	public int getRowOffset(int rowNumber) {
		return _theText.getRowOffset(rowNumber);
	}

	/**
	 * 获取指定行号的第一个字符偏移量。行可以被自动换行成多个列。
	 */
	public int getLineOffset(int lineNumber) {
		return _theText.getLineOffset(lineNumber);
	}

	/**
	 * 将迭代器设置为指向 startingChar。如果 startingChar 无效，
	 * 则 hasNext() 返回 false，并将 _currIndex 设置为 -1。
	 */
	public int seekChar(int startingChar) {
		if (_theText.isValid(startingChar)) {
			_currIndex = startingChar;
		} else {
			_currIndex = -1;
		}
		return _currIndex;
	}

	// 判断是否存在下一个字符
	public boolean hasNext() {
		return (_currIndex >= 0 &&
				_currIndex < _theText.getTextLength());
	}

	/**
	 * 返回下一个字符，并将迭代器向前移动。
	 * 不执行边界检查，调用者应先检查 hasNext()。
	 */
	public char next() {
		char nextChar = _theText.charAt(_currIndex);
		++_currIndex;
		return nextChar;
	}

	/**
	 * 在指定位置插入字符 c，之后的字符向右移动
	 * 如果插入位置无效，则不进行操作。
	 */
	public void insertBefore(char c, int insertionPoint, long timestamp) {
		if (!_theText.isValid(insertionPoint)) {
			return;
		}

		char[] a = new char[1];
		a[0] = c;
		_theText.insert(a, insertionPoint, timestamp, true);
	}

	/**
	 * 在指定位置插入字符数组 cArray，之后的字符向右移动
	 * 如果插入位置无效或 cArray 为空，则不进行操作。
	 */
	public void insertBefore(char[] cArray, int insertionPoint, long timestamp) {
		if (!_theText.isValid(insertionPoint) || cArray.length == 0) {
			return;
		}

		_theText.insert(cArray, insertionPoint, timestamp, true);
	}

	// 插入字符
	public void insert(int i, CharSequence s) {
		_theText.insert(new char[]{s.charAt(0)}, i, System.nanoTime(), true);
	}

	/**
	 * 删除指定位置的字符，如果位置无效则不进行操作。
	 */
	public void deleteAt(int deletionPoint, long timestamp) {
		if (!_theText.isValid(deletionPoint)) {
			return;
		}
		_theText.delete(deletionPoint, 1, timestamp, true);
	}

	/**
	 * 删除从指定位置开始的最多 maxChars 个字符，
	 * 如果位置无效或 maxChars 小于等于0，则不进行操作。
	 */
	public void deleteAt(int deletionPoint, int maxChars, long time) {
		if (!_theText.isValid(deletionPoint) || maxChars <= 0) {
			return;
		}
		int totalChars = Math.min(maxChars, _theText.getTextLength() - deletionPoint);
		_theText.delete(deletionPoint, totalChars, time, true);
	}

	// 返回底层文本缓冲区是否处于批处理编辑模式
	public boolean isBatchEdit() {
		return _theText.isBatchEdit();
	}

	// 开始一系列插入/删除操作，可作为一个整体进行撤销/重做
	public void beginBatchEdit() {
		_theText.beginBatchEdit();
	}

	// 结束一系列插入/删除操作
	public void endBatchEdit() {
		_theText.endBatchEdit();
	}

	// 返回文档中的行数
	public int getRowCount() {
		return _theText.getRowCount();
	}

	// 返回指定行的字符数量
	public int getRowSize(int rowNumber) {
		return _theText.getRowSize(rowNumber);
	}

	// 返回文档中的字符数量，包括文件结束字符
	public int docLength() {
		return _theText.getTextLength();
	}

	// 清除文档中的跨度
	public void clearSpans() {
		_theText.clearSpans();
	}

	// 获取文档中的跨度列表
	public List<Pair> getSpans() {
		return _theText.getSpans();
	}

	/**
	 * 设置文档中的跨度。
	 * Spans 是具有相同格式（如颜色、字体等）的连续字符序列。
	 */
	public void setSpans(List<Pair> spans) {
		_theText.setSpans(spans);
	}

	// 设置文本度量信息
	public void setMetrics(Document.TextFieldMetrics metrics) {
		_theText.setMetrics(metrics);
	}

	// 启用/禁用文档的自动换行
	public void setWordWrap(boolean enable) {
		_theText.setWordWrap(enable);
	}

	// 判断文档是否启用了自动换行
	public boolean isWordWrap() {
		return _theText.isWordWrap();
	}

	// 分析文档中的自动换行点
	public void analyzeWordWrap() {
		_theText.analyzeWordWrap();
	}

	// 判断是否可以撤销
	public boolean canUndo() {
		return _theText.canUndo();
	}

	// 判断是否可以重做
	public boolean canRedo() {
		return _theText.canRedo();
	}

	// 执行撤销操作
	public int undo() {
		return _theText.undo();
	}

	// 执行重做操作
	public int redo() {
		return _theText.redo();
	}

	// 文本变化监听器
	public interface OnTextChangeListener {
		Document.TextFieldMetrics onTextChange(Document.TextFieldMetrics metrics);
	}


	@Override
	public String toString() {
		return _theText.toString();
	}

}