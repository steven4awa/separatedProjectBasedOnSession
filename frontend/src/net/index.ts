import axios, {AxiosError} from 'axios'
import {ElMessage} from  "element-plus";

const defaultError = ()=> ElMessage.warning(AxiosError.name)
const defaultFailure = () => ElMessage.warning("Something went wrong, please contact the administrator")
function post(url: string, data: object,
              success: (message: string, status: number) => void,
              failure = defaultFailure,
              err = defaultError) {
    axios.post(url, data,{
        headers: { // config Request Header
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', // the data will be sent looking like a traditional HTML form submission
        },
        withCredentials: true // include cookies automatically in the request
    }).then(({data})=>{ // 解构 data
        if(data.success){
            success(data.message, data.status)
        } else{
            failure()
        }
    }).catch(err)
}

function get(url: string,
              success: (message: string, status: number) => void,
              failure = defaultFailure,
              err = defaultError) {
    axios.post(url, {
        withCredentials: true // include cookies automatically in the request
    }).then(({data})=>{ // 解构 data
        if(data.success){
            success(data.message, data.status)
        } else{
            failure()
        }
    }).catch(err)
}

export {get, post}