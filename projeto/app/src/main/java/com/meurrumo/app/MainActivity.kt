package com.meurrumo.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant

data class Tx(val kind:String,val desc:String,val value:Double,val cat:String,val due:String)
data class Study(val subject:String,val minutes:Int,val date:String)

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App(applicationContext)}}
}

@Composable fun App(ctx:Context){
 val p=remember{ctx.getSharedPreferences("mr",0)}
 val scope=rememberCoroutineScope()
 var tab by remember{mutableIntStateOf(0)}
 var steps by remember{mutableLongStateOf(p.getLong("steps",0))}
 var salary by remember{mutableStateOf(p.getString("salary","")?:"")}
 var tx by remember{mutableStateOf(loadTx(p))}
 var sub by remember{mutableStateOf(loadStudies(p))}
 var subject by remember{mutableStateOf("")}
 var mins by remember{mutableStateOf("30")}
 var desc by remember{mutableStateOf("")}
 var value by remember{mutableStateOf("")}
 var cat by remember{mutableStateOf("Casa")}
 var due by remember{mutableStateOf("")}

 val hc=remember{HealthConnectClient.getOrCreate(ctx)}
 val perm=remember{HealthPermission.getReadPermission(StepsRecord::class)}
 var status by remember{mutableStateOf("Passos não sincronizados")}
 val launcher=rememberLauncherForActivityResult(
  hc.permissionController.createRequestPermissionResultContract()
 ){g->if(perm in g)scope.launch{steps=readSteps(hc);p.edit().putLong("steps",steps).apply();status="Sincronizado com Health Connect"}}

 MaterialTheme{
  Scaffold(topBar={TopAppBar(title={Text("MEU RUMO V4")})},bottomBar={
   NavigationBar{listOf("Hoje","Corpo","Dinheiro","Estudos","Metas").forEachIndexed{i,n->
    NavigationBarItem(tab==i,{tab=i},{Text(listOf("🏠","💪","💰","📚","🎯")[i])},{Text(n)})
   }}
  }){pad->
   LazyColumn(Modifier.padding(pad).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
    item{Text("Boa noite 👋",style=MaterialTheme.typography.headlineMedium)}
    if(tab==0){
     item{Card{Column(Modifier.padding(16.dp)){Text("👣 Passos");Text(steps.toString(),style=MaterialTheme.typography.displaySmall);Text("Meta 10.000");LinearProgressIndicator({(steps/10000f).coerceIn(0f,1f)},Modifier.fillMaxWidth());Text(status);Button({launcher.launch(setOf(perm))}){Text("Sincronizar")}}}}
     item{Card{Column(Modifier.padding(16.dp)){Text("💰 Financeiro");Text("Salário: R$ ${salary.ifBlank{"0,00"}}");Text("Saldo: ${moneyBalance(tx,salary)}")}}}
     item{Card{Column(Modifier.padding(16.dp)){Text("📚 Estudos");Text("${sub.sumOf{it.minutes}} minutos registrados")}}}
    }
    if(tab==1) item{Card{Column(Modifier.padding(16.dp)){Text("💪 Corpo",style=MaterialTheme.typography.titleLarge);Text("Peso atual: 87 kg");Text("Meta: 82 kg");Text("Passos: $steps");Text("Calorias: 2.200 kcal")}}}
    if(tab==2){
     item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
      Text("💰 Financeiro completo",style=MaterialTheme.typography.titleLarge)
      OutlinedTextField(salary,{salary=it},label={Text("Salário CLT mensal")},modifier=Modifier.fillMaxWidth())
      Button({p.edit().putString("salary",salary).apply()}){Text("Salvar salário")}
      OutlinedTextField(desc,{desc=it},label={Text("Descrição")},modifier=Modifier.fillMaxWidth())
      OutlinedTextField(value,{value=it},label={Text("Valor")},modifier=Modifier.fillMaxWidth())
      OutlinedTextField(cat,{cat=it},label={Text("Categoria")},modifier=Modifier.fillMaxWidth())
      OutlinedTextField(due,{due=it},label={Text("Vencimento (ex. 10/09)")},modifier=Modifier.fillMaxWidth())
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({addTx("gasto",p,tx,desc,value,cat,due){tx=it};desc="";value=""}){Text("+ Gasto")};Button({addTx("salário",p,tx,desc,value,cat,due){tx=it};desc="";value=""}){Text("+ Salário")}}
     }}}
     item{Card{Column(Modifier.padding(16.dp)){val inc=tx.filter{it.kind=="salário"}.sumOf{it.value};val exp=tx.filter{it.kind=="gasto"}.sumOf{it.value};Text("Receitas: R$ %.2f".format(inc));Text("Gastos: R$ %.2f".format(exp));Text("Saldo: R$ %.2f".format(inc-exp));Text("Limite diário estimado: R$ %.2f".format(((inc-exp)/30).coerceAtLeast(0.0)))}}}
     items(tx){x->Text("${if(x.kind=="gasto")"💸" else "💵"} ${x.desc} • R$ %.2f • ${x.cat}${if(x.due.isNotBlank())" • vence ${x.due}" else ""}".format(x.value))}
    }
    if(tab==3){
     item{Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
      Text("📚 Estudos",style=MaterialTheme.typography.titleLarge)
      OutlinedTextField(subject,{subject=it},label={Text("Matéria")},modifier=Modifier.fillMaxWidth())
      OutlinedTextField(mins,{mins=it},label={Text("Minutos")},modifier=Modifier.fillMaxWidth())
      Button({if(subject.isNotBlank()){sub=sub+Study(subject,mins.toIntOrNull()?:30,LocalDate.now().toString());saveStudies(p,sub);subject=""}}){Text("Registrar sessão")}
      Text("Hoje: ${sub.filter{it.date==LocalDate.now().toString()}.sumOf{it.minutes}} min")
     }}}
     items(sub){s->Text("📖 ${s.subject} — ${s.minutes} min — ${s.date}")}
    }
    if(tab==4)item{Card{Column(Modifier.padding(16.dp)){Text("🎯 Metas",style=MaterialTheme.typography.titleLarge);Text("💪 Corpo — 65%");Text("💰 Finanças — 20%");Text("📚 Estudos — 10%");Text("👣 Passos — ${((steps/10000.0)*100).toInt().coerceAtMost(100)}%")}}}
   }
  }
 }
}

fun addTx(k:String,p:android.content.SharedPreferences,list:List<Tx>,d:String,v:String,c:String,due:String,done:(List<Tx>)->Unit){
 val n=v.replace(",",".").toDoubleOrNull()?:return
 val x=list+Tx(k,d.ifBlank{if(k=="gasto")"Gasto" else "Salário"},n,c,due)
 p.edit().putString("tx",x.joinToString("|"){"${it.kind};${it.desc};${it.value};${it.cat};${it.due}"}).apply();done(x)
}
fun loadTx(p:android.content.SharedPreferences):List<Tx>{
 val r=p.getString("tx","")?:"";if(r.isBlank())return emptyList()
 return r.split("|").mapNotNull{a->val x=a.split(";");if(x.size>=5)Tx(x[0],x[1],x[2].toDoubleOrNull()?:0.0,x[3],x[4])else null}
}
fun loadStudies(p:android.content.SharedPreferences):List<Study>{
 val r=p.getString("study","")?:"";if(r.isBlank())return emptyList()
 return r.split("|").mapNotNull{a->val x=a.split(";");if(x.size>=3)Study(x[0],x[1].toIntOrNull()?:0,x[2])else null}
}
fun saveStudies(p:android.content.SharedPreferences,l:List<Study>){p.edit().putString("study",l.joinToString("|"){"${it.subject};${it.minutes};${it.date}"}).apply()}
fun moneyBalance(t:List<Tx>,s:String):String{val inc=t.filter{it.kind=="salário"}.sumOf{it.value}+(s.replace(",",".").toDoubleOrNull()?:0.0);val exp=t.filter{it.kind=="gasto"}.sumOf{it.value};return "R$ %.2f".format(inc-exp)}
suspend fun readSteps(c:HealthConnectClient):Long{
 val z=ZoneId.systemDefault();val start=LocalDate.now(z).atStartOfDay(z).toInstant()
 val r=c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL),TimeRangeFilter.between(start,Instant.now())))
 return r[StepsRecord.COUNT_TOTAL]?:0L
}
