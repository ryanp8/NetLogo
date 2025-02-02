// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.ide

import java.awt.{ Color, Component, Cursor, Point }
import java.awt.event.{ KeyAdapter, KeyEvent, MouseAdapter, MouseEvent, WindowFocusListener, WindowEvent }
import javax.swing.{ BorderFactory, JDialog, JEditorPane, JScrollPane, JTable, SwingConstants, UIDefaults, UIManager }
import javax.swing.table.{ DefaultTableCellRenderer, DefaultTableModel, TableCellRenderer }
import javax.swing.text.BadLocationException

import org.nlogo.core.{ Femto, Token, TokenType, TokenizerInterface }
import org.nlogo.api.Exceptions
import org.nlogo.editor.{ AbstractEditorArea, Colorizer, HighlightEditorKit, RichDocument }, RichDocument.RichDoc

class ShowUsageBox(colorizer: Colorizer) {

  val usageBox = new JDialog()
  var editorArea: AbstractEditorArea = null
  def document = editorArea.getDocument
  val dataModel = new DefaultTableModel(){
    override def isCellEditable(row: Int, column: Int): Boolean = false
    override def getColumnClass(columnIndex: Int): Class[_] = {
      if(columnIndex == 0) classOf[Token] else classOf[String]
    }
  }
  val usageTable = new JTable(dataModel)
  val scrollPane = new JScrollPane(usageTable)
  usageBox.add(scrollPane)
  usageBox.setUndecorated(true)
  usageTable.setDefaultRenderer(classOf[Token], new LineNumberRenderer())
  usageTable.setDefaultRenderer(classOf[String], new LineRenderer(None))

  usageBox.addWindowFocusListener(new WindowFocusListener {
    override def windowLostFocus(e: WindowEvent): Unit = {
      usageBox.setVisible(false)
      dataModel.synchronized(dataModel.setRowCount(0))
    }

    override def windowGainedFocus(e: WindowEvent): Unit = {}
  })
  dataModel.setColumnCount(2)
  usageTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION)
  usageTable.getColumnModel.getColumn(0).setMaxWidth(40)
  usageTable.getColumnModel.setColumnSelectionAllowed(false)
  usageTable.getTableHeader().setReorderingAllowed(false)
  usageTable.setTableHeader(null)
  usageTable.setRowMargin(3)
  usageTable.setRowHeight(20)
  usageTable.getColumnModel.getColumn(1).setMinWidth(300)
  usageTable.getColumnModel.getColumn(0).setMinWidth(40)
  usageTable.setShowGrid(false)

  def init(editorArea: AbstractEditorArea) {
    if(this.editorArea == null) {
      this.editorArea = editorArea

      usageTable.addMouseListener(new MouseAdapter() {
        override def mouseClicked(e: MouseEvent): Unit = {
          usageBox.setVisible(false)
          val token = usageTable.getValueAt(usageTable.getSelectedRow, 0).asInstanceOf[Token]
          editorArea.containingViewport.foreach { scrollPane =>
            try {
              val r = editorArea.modelToView2D(token.start)
              val scrollHeight = scrollPane.getExtentSize.height
              val viewHeight   = scrollPane.getViewSize.height
              val y = (0 max r.getY.toInt - ((scrollHeight - r.getHeight.toInt) / 2)) min (viewHeight - scrollHeight)
              scrollPane.setViewPosition(new Point(0, y))
            } catch {
              case ex: BadLocationException => Exceptions.ignore(ex)
            }
          }
          editorArea.select(token.start, token.end)
        }
      })

      usageTable.addKeyListener(new KeyAdapter() {
        override def keyPressed(e: KeyEvent): Unit = {
          e.getKeyCode match {
            case KeyEvent.VK_ESCAPE => usageBox.setVisible(false)
            case _ => e.setSource(editorArea)
              editorArea.dispatchEvent(e)
          }
        }
      })
    }
  }

  usageTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))

  def showBox(popupLocation: Point, cursorPosition: Int): Unit = {
    val tokenOption = JumpToDeclaration.findTokenContainingPosition(editorArea.getText(), cursorPosition)
    for { token <- tokenOption } {
      if (token.tpe == TokenType.Ident || token.tpe == TokenType.Command || token.tpe == TokenType.Reporter) {
        val tokens = getUsage(editorArea.getText(), token)
        dataModel.setRowCount(0)
        for {
          t    <- tokens
          line <- document.getLineText(document.offsetToLine(t.start))
        } {
          dataModel.addRow(Array[AnyRef](t, line.trim))
        }
        if (dataModel.getRowCount != 0) {
          usageTable.setDefaultRenderer(classOf[String], new LineRenderer(Some(token.text)))
          usageTable.setPreferredScrollableViewportSize(usageTable.getPreferredSize())
          usageTable.setFillsViewportHeight(true)
          usageBox.pack()
          usageTable.validate()
          usageBox.setLocation(popupLocation)
          usageBox.setVisible(true)
        }
      }
    }
  }

  def getUsage(source: String, token: Token): Seq[Token] = {
    val iterator = Femto.scalaSingleton[TokenizerInterface]("org.nlogo.lex.Tokenizer").tokenizeString(source)
    var iter = scala.collection.mutable.Seq[Token]()
    var prevLineNo = -1
    val tokenLineNo = document.offsetToLine(token.start)
    for(t <- iterator if t.text.equalsIgnoreCase(token.text) &&
        document.offsetToLine(t.start) != tokenLineNo &&
        document.offsetToLine(t.start) != prevLineNo) {
      iter :+= t
      prevLineNo = document.offsetToLine(t.start)
    }
    iter
  }

  class LineRenderer(boldedString: Option[String]) extends TableCellRenderer {
    override def getTableCellRendererComponent(table: JTable, value: AnyRef, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component = {
      val pane  = new JEditorPane()
      pane.setOpaque(true)
      pane.setBorder(BorderFactory.createEmptyBorder(1,0,0,0))
      val editorKit = boldedString match {
        case None                 => new HighlightEditorKit(colorizer)
        case Some(selectedString) => new BoldEditorKit(selectedString)
      }
      pane.setEditorKit(editorKit)
      pane.setText(value.asInstanceOf[String])
      val alternate = UIManager.getColor("Table.alternateRowColor")
      val defaults = new UIDefaults()
      if (row%2 == 0){
        defaults.put("EditorPane[Enabled].backgroundPainter", Color.WHITE)
        pane.setBackground(Color.WHITE)
      }
      else {
        defaults.put("EditorPane[Enabled].backgroundPainter", alternate)
        pane.setBackground(alternate)
      }
      if(isSelected) {
        pane.setBackground(usageTable.getSelectionBackground)
        pane.setForeground(usageTable.getSelectionForeground)
      }
      pane.putClientProperty("Nimbus.Overrides", defaults)
      pane.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
      pane.setFont(org.nlogo.awt.Fonts.monospacedFont)
      pane
    }
  }
  class LineNumberRenderer extends DefaultTableCellRenderer {
    override def setValue(value: AnyRef) = {
      if (value != null) {
        setText((document.offsetToLine(value.asInstanceOf[Token].start) + 1).toString)
        setHorizontalAlignment(SwingConstants.RIGHT)
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8))
      } else
        super.setValue(null)
    }
  }
  import org.nlogo.editor.{ HighlightEditorKit, HighlightView }

  class BoldEditorKit(selectedString: String) extends HighlightEditorKit(colorizer) {

    import scala.collection.immutable.Range

    class BoldView(elem: javax.swing.text.Element) extends HighlightView(this.pane, elem, this.colorizer) {
      var boldingRanges = Seq[Range]()

      override def studyLine(lineIndex: Int): Unit = {
        super.studyLine(lineIndex)
        val elem = getElement.getElement(lineIndex)
        val lineText = getDocument.getText(elem.getStartOffset, elem.getEndOffset - elem.getStartOffset max 0)
        val iterator = Femto.scalaSingleton[TokenizerInterface]("org.nlogo.lex.Tokenizer").tokenizeString(lineText)
        for(token <- iterator) {
          if(token.text.equalsIgnoreCase(selectedString)){
            boldingRanges :+= (token.start to token.end)
          }
        }
      }

      override def drawText(g: java.awt.Graphics2D, x: Float, y: Float, p0: Int, p1: Int, isSelected: Boolean): Float = {
        g match {
          case g2d: java.awt.Graphics2D =>
            g2d.setRenderingHint(
                      java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                      java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_GASP)
          case _ =>
        }
        var endX = super.drawText(g, x, y, p0, boldingRanges.head.start, isSelected)
        val originalFont = g.getFont
        val boldFont = originalFont.deriveFont(java.awt.Font.BOLD)
        for (i <- boldingRanges.indices) {
          g.setFont(boldFont)
          endX = super.drawText(g, endX, y, boldingRanges(i).start, boldingRanges(i).end, isSelected)
          g.setFont(originalFont)
          if(i < boldingRanges.length - 1) {
            endX = super.drawText(g, endX, y, boldingRanges(i).end, boldingRanges(i + 1).start, isSelected)
          }
        }
        g.setFont(originalFont)
        super.drawText(g, endX, y, boldingRanges.last.end, p1, isSelected)
      }
    }
    override def create(elem: javax.swing.text.Element): javax.swing.text.View = new BoldView(elem)
  }
}
