import re

def extract_test_data_pipeline():
    return [
    {'$match':{'_id.type':{'$in':['TAR','TED.SPEC',"TED-SPEC",'SPEC_MUTATION']}}},
    {'$group':{'_id':'$_id.model_id','s':{'$push':'$$ROOT'},'c':{'$sum':1}}},
    {'$match':{'c':3}},
    {'$unwind':'$s'},
    {'$replaceRoot':{'newRoot':'$s'}},
    {'$addFields':{
        'combinator':{'$multiply':[
            {'$cond':{'if':'$data.success','then':1,'else':0}},
            {'$switch':{'branches':[
                {'case':{'$eq':['$_id.type','SPEC_MUTATION']},'then':2},
                {'case':{'$eq':['$_id.type','TED.SPEC']},'then':1},
                {'case':{'$eq':['$_id.type','TAR']},'then':4}],
                'default':0}
            }]
        }
    }},
    {'$facet':
        {
            'combinations':[
                {'$group':{'_id':{'model_id':'$_id.model_id','graph_id':'$graphId'},'combination':{'$sum':'$combinator'}}},
                {'$group':{'_id':{'combination':'$combination','graph_id':'$_id.graph_id'},'sum':{'$sum':1}}},
                {'$group':{'_id':'$_id.graph_id','combination':{'$push':{'k':{'$toString':'$_id.combination'},'v':'$sum'}},'count':{'$sum':'$sum'}}},
                {'$project':{'_id':'$_id','combination':{'$arrayToObject':'$combination'},'count':'$count'}}],
            'times':[
                {'$group':{'_id':{'graph_id':'$graphId','type':'$_id.type'},'count':{'$sum':1},'sum':{'$sum':'$data.time'},'avg':{'$avg':'$data.time'},'stddev':{'$stdDevPop':'$data.time'}}},
                {'$group':{'_id':'$_id.graph_id','tests':{'$push':{'k':'$_id.type','v':{'count':'$count','sum':'$sum','avg':'$avg','stddev':'$stddev'}}}}},
                {'$addFields':{'tests':{'$arrayToObject':'$tests'}}}]
        }
    },
    {'$project':{'all':{'$concatArrays':['$combinations','$times']}}},
    {'$unwind':'$all'},
    {'$replaceRoot':{'newRoot':'$all'}},
    {'$group':{'_id':'$_id','comb':{'$push':'$combination'},'count':{'$push':'$count'},'tests':{'$push':'$tests'}}},
    {'$unwind':'$count'},
    {'$unwind':'$comb'},
    {'$unwind':'$tests'},
    {'$lookup':{'from':'Graph','localField':'_id','foreignField':'_id','as':'graph','pipeline':[{'$addFields':{'parsing':{'$objectToArray':'$parsing'}}},
    {'$unwind':'$parsing'},
    {'$group':{'_id':'$_id','name':{'$push':'$name'},'policy_time':{'$sum':'$policy.time'},'policy_count':{'$sum':'$policy.count'},'parsing_time':{'$sum':'$parsing.v.time'},'parsing_count':{'$sum':'$parsing.v.count'}}},
    {'$addFields':{'name':{'$first':'$name'}}}]}},
    {'$unwind':{'path':'$graph'}},
    {'$addFields':{'reg':{'$regexFind':{'input':'$graph.name','regex':re.compile(r"([^-]*)-.*")}}}},
    {'$addFields':{'reg':{'$first':'$reg.captures'}}},
    {'$group':{
        '_id':'$reg',
        'NONE':{'$sum':'$comb.0'},
        'SPEC':{'$sum':'$comb.1'},
        'MUT':{'$sum':'$comb.2'},
        'SPEC & MUT':{'$sum':'$comb.3'},
        'TAR':{'$sum':'$comb.4'},
        'TAR & SPEC':{'$sum':'$comb.5'},
        'TAR & MUT':{'$sum':'$comb.6'},
        'TAR & SPEC & MUT':{'$sum':'$comb.7'},
        'count':{'$sum':'$count'},
        'parsing_time':{'$avg':'$graph.parsing_time'},
        'parsing_count':{'$sum':'$graph.parsing_count'},
        'policy_time':{'$sum':'$graph.policy_time'},
        'policy_count':{'$sum':'$graph.policy_count'},
        'TAR_TIME':{'$avg':'$tests.TAR.avg'},
        'TAR_DEV':{'$avg':'$tests.TAR.stddev'},
        'MUT_TIME':{'$avg':'$tests.SPEC_MUTATION.avg'},
        'MUT_DEV':{'$avg':'$tests.SPEC_MUTATION.stddev'}
        }
    }
]