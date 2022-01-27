// This file is where all of the CPU components are assembled into the whole CPU

package dinocpu

import chisel3._
import chisel3.util._
import dinocpu.components._
import scala.annotation.switch

/**
 * The main CPU definition that hooks up all of the other components.
 *
 * For more information, see section 4.4 of Patterson and Hennessy
 * This follows figure 4.21
 */
class SingleCycleCPU(implicit val conf: CPUConfig) extends BaseCPU {
  // All of the structures required
  val pc         = dontTouch(RegInit(0.U))
  val control    = Module(new Control())
  val registers  = Module(new RegisterFile())
  val aluControl = Module(new ALUControl())
  val alu        = Module(new ALU())
  val immGen     = Module(new ImmediateGenerator())
  val nextpc     = Module(new NextPC())
  //val pcAdder    = Module(new Adder())
  val (cycleCount, _) = Counter(true.B, 1 << 30)

  // Should be removed when wired are connected
  //immGen.io     := DontCare
  //nextpc.io     := DontCare
  //io.dmem       := DontCare

  //FETCH
  io.imem.address := pc
  io.imem.valid := true.B

  val instruction = io.imem.instruction

  //CONTROL
  control.io.opcode := instruction(6, 0)

  registers.io.readreg1 := instruction(19, 15)
  registers.io.readreg2 := instruction(24, 20)
  registers.io.writereg := instruction(11, 7)
  when(control.io.toreg){
    registers.io.writedata := io.dmem.readdata
  }.otherwise{
    when (control.io.resultselect){
      registers.io.writedata := immGen.io.sextImm
    }.otherwise{
      registers.io.writedata := alu.io.result
    }
  }
  //registers.io.writedata := alu.io.result
  when (registers.io.writereg =/= 0.U) {
    registers.io.wen := control.io.regwrite//equal to write command from cpu if writereg is not equal to zero
  } .otherwise {
    registers.io.wen := false.B
  }


  aluControl.io.aluop := control.io.aluop
  aluControl.io.itype := control.io.itype
  aluControl.io.funct7 := instruction(31, 25) //FUNCT7
  aluControl.io.funct3 := instruction(14, 12) //FUNCT3
  aluControl.io.wordinst := control.io.wordinst

  alu.io.operation := aluControl.io.operation
  when (control.io.src1){
    alu.io.inputx := pc
  }.otherwise{
  alu.io.inputx := registers.io.readdata1
  }
  //put mux here
  when (control.io.src2 === "b00".U){
    alu.io.inputy := registers.io.readdata2
  }.elsewhen(control.io.src2 === "b01".U){
    alu.io.inputy := immGen.io.sextImm
  }.otherwise{
    alu.io.inputy := 4.U //hardwire to 4
  }
  //alu.io.inputy := registers.io.readdata2

  //pcAdder.io.inputx := pc
  //pcAdder.io.inputy := 4.U
  //pc := pcAdder.io.result

  nextpc.io.funct3 := instruction(14, 12)
  nextpc.io.jumptype := control.io.jumptype
  nextpc.io.inputx := registers.io.readdata1
  nextpc.io.inputy := registers.io.readdata2
  nextpc.io.pc := pc
  nextpc.io.branch := control.io.branch //for now
  nextpc.io.imm := immGen.io.sextImm

  immGen.io.instruction := io.imem.instruction

  //dmem
  io.dmem.address := alu.io.result
  when (instruction (14, 12) < 4.U){
    io.dmem.maskmode := instruction(14, 12)
  }.otherwise{
    val actual_mask = instruction(14, 12) - 4.U
    io.dmem.maskmode := actual_mask
  }
  
  io.dmem.sext := instruction (14, 12)
  io.dmem.writedata := registers.io.readdata2
  when (control.io.memop === 2.U){
    io.dmem.memread := true.B
    io.dmem.memwrite := false.B
  }.elsewhen (control.io.memop ===3.U){
    io.dmem.memread := false.B
    io.dmem.memwrite := true.B
  }.otherwise{
    io.dmem.memread := false.B
    io.dmem.memwrite := false.B
  }
  io.dmem.valid := true.B
  when (control.io.opcode === "b0000011".U ){
    alu.io.operation := "b00111".U //manually alters funct 3 for alucontrol tho theres gotta be a way to do without this
    
  }.elsewhen (control.io.opcode ==="b0010111".U){
    //for u types
    alu.io.operation := "b00111".U 
    alu.io.inputx := pc
    alu.io.inputy := immGen.io.sextImm
    registers.io.writereg := instruction (11, 7)
    registers.io.writedata := alu.io.result
  }.elsewhen (control.io.opcode === "b0100011".U){
    alu.io.operation := "b00111".U
    //alu.io.inputx := registers.io.readdata1
    //alu.io.inputy := immGen.io.sextImm
    //registers.io.writereg := instruction (24, 20)
    ///io.dmem.address := alu.io.result
    //registers.io.writedata := io.dmem.readdata
  }
  when (nextpc.io.jumptype === "b10".U){
    //jalr
    //nextpc.io.inputx := pc
    //nextpc.io.inputy := immGen.io.sextImm
    //registers.io.writereg := instruction (11, 7)

    alu.io.operation := "b00111".U
    //nextpc.io.funct3 := "b010".U
    //registers.io.writedata := alu.io.result
  }.elsewhen(nextpc.io.jumptype === "b11".U){
    //jalr
    //nextpc.io.inputx := pc
    //nextpc.io.inputy := immGen.io.sextImm
    //registers.io.writereg := instruction (11, 7)
    alu.io.operation := "b00111".U
  }


  pc := nextpc.io.nextpc //connecting next pc to pc
}

/*
 * Object to make it easier to print information about the CPU
 */
object SingleCycleCPUInfo {
  def getModules(): List[String] = {
    List(
      "dmem",
      "imem",
      "control",
      "registers",
      "csr",
      "aluControl",
      "alu",
      "immGen",
      "nextpc"
    )
  }
}
