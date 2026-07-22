import axios, {AxiosError} from 'axios'
import {ElMessage} from  "element-plus";

const defaultError = (err: AxiosError) => {
    const data = err.response?.data as any
    if (data?.message) {
        ElMessage.warning(data.message)
    } else {
        ElMessage.warning("Network error, please try again later")
    }
}
const defaultFailure = (message?: string) => {
    if (message) {
        ElMessage.warning(message)
    } else {
        ElMessage.warning("Something went wrong, please contact the administrator")
    }
}
function post(url: string, data: Record<string, any>,
              success: (message: string, status: number) => void,
              failure: (message: string, status: number) => void = defaultFailure,
              err = defaultError) {
    // URL-encode the data so Spring can bind @RequestParam parameters
    const params = new URLSearchParams();
    for (const key in data) {
        params.append(key, data[key]);
    }
    axios.post(url, params,{
        headers: { // config Request Header
            'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8', // the data will be sent looking like a traditional HTML form submission
        },
        withCredentials: true // include cookies automatically in the request
    }).then(({data})=>{ // 解构 data
        if(data.success){
            success(data.message, data.status)
        } else{
            failure(data.message, data.status)
        }
    }).catch(err)
}

function get(url: string,
              success: (message: string, status: number) => void,
              failure = defaultFailure,
              err = defaultError) {
    axios.get(url, {
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