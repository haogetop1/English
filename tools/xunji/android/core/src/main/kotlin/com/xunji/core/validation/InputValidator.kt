package com.xunji.core.validation
class FieldValidationException(val field:String,message:String):IllegalArgumentException(message)
object InputValidator {
    fun deckName(raw:String):String {
        val name=raw.trim()
        check("name",name,80,true)
        return name
    }
    fun cardContent(front:String,back:String,example:String) {
        check("front",front,2000,true)
        check("back",back,10000,true)
        check("example",example,4000,false)
    }
    private fun check(field:String,text:String,maximum:Int,required:Boolean) {
        if(required && text.isBlank()) throw FieldValidationException(field,"请填写此项")
        if(text.codePointCount(0,text.length)>maximum) throw FieldValidationException(field,"最多填写 $maximum 个字符")
    }
}
